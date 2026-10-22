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

        return readFrom(reader);
    }

    /**
     * Читает CSV и закрывает переданный reader, в том числе при ошибке.
     */
    private List<Expense> readFrom(BufferedReader reader) {
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

    private List<Expense> readRecords(CSVParser parser) {
        List<Expense> expenses = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();

        for (CSVRecord record : parser) {
            Expense expense = readRecord(record);
            validateUniqueId(ids, expense.getId(), record.getRecordNumber());
            expenses.add(expense);
        }

        return expenses;
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

    private void validateUniqueId(Set<UUID> ids, UUID id, long recordNumber) {
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