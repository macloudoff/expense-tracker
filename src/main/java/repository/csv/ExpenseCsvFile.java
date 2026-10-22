package repository.csv;

import model.Expense;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import repository.ExpenseRepositoryException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Читает и записывает CSV-файл расходов.
 *
 * <p>Каталог должен существовать. Отсутствующий файл означает пустое
 * хранилище. Существующий файл должен содержать корректный заголовок.
 *
 * <p>Запись выполняется через атомарную замену основного файла.
 * Одновременная работа нескольких писателей не поддерживается.
 */
final class ExpenseCsvFile {

    private static final String FILENAME = "expenses.csv";

    private static final CSVFormat READ_FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();

    private static final CSVFormat WRITE_FORMAT = CSVFormat.RFC4180.builder()
            .setHeader(ExpenseCsvMapper.HEADER.toArray(String[]::new))
            .get();

    private final Path directory;
    private final Path file;

    ExpenseCsvFile(Path directory) {
        this.directory = validateDirectory(directory);
        this.file = this.directory.resolve(FILENAME);
    }

    private static Path validateDirectory(Path directory) {
        Objects.requireNonNull(directory, "Directory must not be null");

        Path normalized = directory.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException(String.format(
                    "Path is not an existing directory: %s",
                    normalized
            ));
        }

        return normalized;
    }

    List<Expense> read() {
        BufferedReader reader;

        try {
            reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw storageError("Failed to open expenses file", e);
        }

        ReadResult result = readFrom(reader);
        if (result.legacy()) {
            backupLegacyFile();
            write(result.expenses());
        }
        return result.expenses();
    }

    /**
     * Читает CSV и закрывает переданный reader, в том числе при ошибке.
     */
    private ReadResult readFrom(BufferedReader reader) {
        try (
                reader;
                CSVParser parser = READ_FORMAT.parse(reader)
        ) {
            validateHeader(parser);
            return readRecords(parser);
        } catch (IOException | UncheckedIOException e) {
            throw storageError("Failed to read expenses", e);
        }
    }

    private void validateHeader(CSVParser parser) {
        if (!parser.getHeaderNames().equals(ExpenseCsvMapper.HEADER)) {
            throw new ExpenseRepositoryException(String.format(
                    "Unexpected CSV header in %s",
                    file
            ));
        }
    }

    private ReadResult readRecords(CSVParser parser) throws IOException {
        List<CSVRecord> records = parser.getRecords();
        List<Expense> expenses = new ArrayList<>();
        Set<String> originalIds = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        Boolean legacy = null;

        for (CSVRecord record : records) {
            String rawId = record.get("id");
            boolean uuid = isLegacyId(rawId);
            if (legacy != null && legacy != uuid) {
                throw new ExpenseRepositoryException("Mixed UUID and numeric IDs in " + file);
            }
            legacy = uuid;
            if (!originalIds.add(uuid ? rawId.toLowerCase(java.util.Locale.ROOT) : rawId)) {
                throw new ExpenseRepositoryException(String.format(
                        "Duplicate expense id %s in %s, record %d",
                        rawId, file, record.getRecordNumber()
                ));
            }
            Expense expense = uuid
                    ? readLegacyRecord(record, expenses.size() + 1L)
                    : readRecord(record);
            validateUniqueId(ids, expense.getId(), record.getRecordNumber());
            expenses.add(expense);
        }
        return new ReadResult(expenses, Boolean.TRUE.equals(legacy));
    }

    private static boolean isLegacyId(String value) {
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Expense readLegacyRecord(CSVRecord record, long id) {
        try {
            return ExpenseCsvMapper.fromRecord(record, id);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new ExpenseRepositoryException(String.format(
                    "Invalid legacy expense in %s, record %d", file, record.getRecordNumber()
            ), e);
        }
    }

    private void backupLegacyFile() {
        try {
            Path backup = Files.createTempFile(directory, "expenses-uuid-backup-", ".csv");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw storageError("Failed to back up legacy CSV; migration cancelled", e);
        }
    }

    private record ReadResult(List<Expense> expenses, boolean legacy) {
    }

    private Expense readRecord(CSVRecord record) {
        try {
            return ExpenseCsvMapper.fromRecord(record);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new ExpenseRepositoryException(
                    String.format(
                            "Invalid expense in %s, record %d",
                            file,
                            record.getRecordNumber()
                    ),
                    e
            );
        }
    }

    private void validateUniqueId(Set<Long> ids, Long id, long recordNumber) {
        if (!ids.add(id)) {
            throw new ExpenseRepositoryException(String.format(
                    "Duplicate expense id %s in %s, record %d",
                    id,
                    file,
                    recordNumber
            ));
        }
    }

    void write(List<Expense> expenses) {
        Objects.requireNonNull(expenses, "Expenses must not be null");

        Path temporaryFile = createTemporaryFile();

        try {
            writeAndReplace(temporaryFile, expenses);
        } catch (RuntimeException | Error failure) {
            deleteTemporaryFile(temporaryFile, failure);
            throw failure;
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile(directory, "expenses-", ".tmp");
        } catch (IOException e) {
            throw storageError("Failed to create temporary file", e);
        }
    }

    private void writeAndReplace(Path temporaryFile, List<Expense> expenses) {
        try {
            writeRecords(temporaryFile, expenses);
            replaceStorageFile(temporaryFile);
        } catch (IOException e) {
            throw storageError("Failed to write expenses", e);
        }
    }

    private void writeRecords(Path target, List<Expense> expenses) throws IOException {
        try (
                CSVPrinter printer = new CSVPrinter(
                        Files.newBufferedWriter(target, StandardCharsets.UTF_8),
                        WRITE_FORMAT
                )
        ) {
            for (Expense expense : expenses) {
                printer.printRecord(ExpenseCsvMapper.toRecord(expense));
            }
        }
    }

    private void replaceStorageFile(Path temporaryFile) throws IOException {
        Files.move(
                temporaryFile,
                file,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private static void deleteTemporaryFile(Path temporaryFile, Throwable originalFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException | SecurityException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }
    }

    private ExpenseRepositoryException storageError(String message, Throwable cause) {
        return new ExpenseRepositoryException(
                String.format("%s: %s", message, file),
                cause
        );
    }
}