package repository.csv;

import repository.ExpenseRepositoryException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Сохраняет последний выданный ID, в том числе после удаления всех расходов. */
final class ExpenseIdSequence {

    private final Path file;

    ExpenseIdSequence(Path directory) {
        file = directory.toAbsolutePath().normalize().resolve("expenses.sequence");
    }

    long next() {
        long previous = read();
        if (previous == Long.MAX_VALUE) {
            throw new ExpenseRepositoryException("Expense ID sequence exhausted");
        }
        long next = previous + 1;
        write(next);
        return next;
    }

    void ensureAtLeast(long value) {
        if (value > read()) {
            write(value);
        }
    }

    private long read() {
        try {
            long value = Long.parseLong(Files.readString(file, StandardCharsets.UTF_8).strip());
            if (value < 0) {
                throw new NumberFormatException("Negative sequence");
            }
            return value;
        } catch (NoSuchFileException e) {
            return 0;
        } catch (IOException | NumberFormatException e) {
            throw new ExpenseRepositoryException("Invalid expense ID sequence: " + file, e);
        }
    }

    private void write(long value) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(file.getParent(), "expenses-sequence-", ".tmp");
            Files.writeString(temporary, Long.toString(value), StandardCharsets.UTF_8);
            Files.move(temporary, file,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException failure) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw new ExpenseRepositoryException("Failed to save expense ID sequence: " + file, failure);
        }
    }
}
