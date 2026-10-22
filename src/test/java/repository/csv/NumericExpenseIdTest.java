package repository.csv;

import model.Expense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.ExpenseRepositoryException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumericExpenseIdTest {

    @TempDir
    Path directory;

    @Test
    void shouldAllocateConsecutiveIdsWithoutMutatingInput() {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        Expense draft = draft();
        Expense first = repository.save(draft);
        Expense second = repository.save(draft());

        assertAll(
                () -> assertNull(draft.getId()),
                () -> assertEquals(1L, first.getId()),
                () -> assertEquals(2L, second.getId()),
                () -> assertEquals(draft.getCreatedAt(), first.getCreatedAt())
        );
    }

    @Test
    void shouldNotRenumberOrReuseDeletedIdsAfterRestart() {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        Expense first = repository.save(draft());
        Expense second = repository.save(draft());
        repository.deleteById(second.getId());

        CsvExpenseRepository reopened = new CsvExpenseRepository(directory);
        Expense third = reopened.save(draft());
        assertEquals(3L, third.getId());
        assertTrue(reopened.findById(first.getId()).isPresent());

        reopened.deleteById(first.getId());
        reopened.deleteById(third.getId());
        assertEquals(4L, new CsvExpenseRepository(directory).save(draft()).getId());
    }

    @Test
    void shouldKeepIdWhenUpdatingSavedExpense() {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        Expense saved = repository.save(draft());
        saved.setAmount(new BigDecimal("30"));
        Expense updated = repository.save(saved);
        assertEquals(saved.getId(), updated.getId());
        assertEquals(1, repository.findAll().size());
        assertEquals(2L, repository.save(draft()).getId());
    }

    @Test
    void shouldMigrateLegacyCsvAndKeepExactBackup() throws IOException {
        String original = """
                id,created_at,description,amount,date,merchant
                123e4567-e89b-12d3-a456-426614174000,2020-01-02T10:00:00Z,Lunch,20,2020-01-01,Cafe
                123e4567-e89b-12d3-a456-426614174001,2020-01-02T11:00:00Z,Dinner,10,2020-01-01,
                """;
        Files.writeString(directory.resolve("expenses.csv"), original);
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);

        List<Expense> migrated = repository.findAll();
        assertEquals(List.of(1L, 2L), migrated.stream().map(Expense::getId).toList());
        assertEquals("Cafe", migrated.getFirst().getMerchant());
        assertNull(migrated.getLast().getMerchant());

        try (var files = Files.list(directory)) {
            List<Path> backups = files.filter(path -> path.getFileName().toString()
                    .startsWith("expenses-uuid-backup-")).toList();
            assertEquals(1, backups.size());
            assertEquals(original, Files.readString(backups.getFirst()));
        }

        repository.deleteById(2L);
        assertEquals(3L, new CsvExpenseRepository(directory).save(draft()).getId());
    }

    @Test
    void shouldNotOverwriteCorruptedLegacyCsv() throws IOException {
        String original = """
                id,created_at,description,amount,date,merchant
                123e4567-e89b-12d3-a456-426614174000,2020-01-02T10:00:00Z,Lunch,0,2020-01-01,Cafe
                """;
        Path file = directory.resolve("expenses.csv");
        Files.writeString(file, original);
        assertThrows(ExpenseRepositoryException.class,
                () -> new CsvExpenseRepository(directory).findAll());
        assertEquals(original, Files.readString(file));
    }

    @Test
    void shouldRejectCorruptedSequenceWithoutChangingExpenses() throws IOException {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        repository.save(draft());
        String original = Files.readString(directory.resolve("expenses.csv"));
        Files.writeString(directory.resolve("expenses.sequence"), "invalid");
        assertThrows(ExpenseRepositoryException.class, () -> repository.save(draft()));
        assertEquals(original, Files.readString(directory.resolve("expenses.csv")));
    }

    @Test
    void shouldRejectExhaustedSequenceWithoutChangingExpenses() throws IOException {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        repository.save(draft());
        String original = Files.readString(directory.resolve("expenses.csv"));
        Files.writeString(directory.resolve("expenses.sequence"), Long.toString(Long.MAX_VALUE));
        assertThrows(ExpenseRepositoryException.class, () -> repository.save(draft()));
        assertEquals(original, Files.readString(directory.resolve("expenses.csv")));
    }

    @Test
    void shouldRejectMixedIdsWithoutMigrating() throws IOException {
        String original = """
                id,created_at,description,amount,date,merchant
                123e4567-e89b-12d3-a456-426614174000,2020-01-02T10:00:00Z,Lunch,20,2020-01-01,Cafe
                2,2020-01-02T11:00:00Z,Dinner,10,2020-01-01,
                """;
        Path file = directory.resolve("expenses.csv");
        Files.writeString(file, original);
        assertThrows(ExpenseRepositoryException.class,
                () -> new CsvExpenseRepository(directory).findAll());
        assertEquals(original, Files.readString(file));
    }

    @Test
    void shouldRejectNonPositiveIds() {
        CsvExpenseRepository repository = new CsvExpenseRepository(directory);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> repository.findById(0L)),
                () -> assertThrows(IllegalArgumentException.class, () -> repository.deleteById(-1L)),
                () -> assertThrows(IllegalArgumentException.class, () -> Expense.reconstitute(
                        0L, java.time.Instant.now(), "Lunch", BigDecimal.ONE,
                        LocalDate.of(2020, 1, 1), null
                ))
        );
    }

    private static Expense draft() {
        return new Expense("Lunch", new BigDecimal("20"), LocalDate.of(2020, 1, 1), null);
    }
}
