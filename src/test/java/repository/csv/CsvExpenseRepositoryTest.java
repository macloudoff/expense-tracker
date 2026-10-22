package repository.csv;

import model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.ExpenseRepositoryException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CsvExpenseRepositoryTest {

    @TempDir
    Path directory;

    private CsvExpenseRepository repository;

    @BeforeEach
    void createRepository() {
        repository = new CsvExpenseRepository(directory);
    }

    // CONSTRUCTOR

    @Test
    void constructorShouldRejectNullDirectory() {
        assertThrows(
                NullPointerException.class,
                () -> new CsvExpenseRepository(null)
        );
    }

    @Test
    void constructorShouldRejectMissingDirectory() {
        Path missing = directory.resolve("missing");

        assertThrows(
                IllegalArgumentException.class,
                () -> new CsvExpenseRepository(missing)
        );
    }

    @Test
    void constructorShouldRejectRegularFile() throws IOException {
        Path file = Files.createFile(directory.resolve("file"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CsvExpenseRepository(file)
        );
    }

    // SAVE

    @Test
    void saveShouldPersistAllFields() {
        Expense expected = createExpense("Продукты", "Магазин");

        repository.save(expected);

        CsvExpenseRepository reopened = new CsvExpenseRepository(directory);
        Expense actual = reopened.findById(expected.getId()).orElseThrow();

        assertExpenseEquals(expected, actual);
    }

    @Test
    void saveShouldPreserveExistingExpenses() {
        Expense first = createExpense("Продукты", "Магазин");
        Expense second = createExpense("Проезд", null);

        repository.save(first);
        repository.save(second);

        assertAll(
                () -> assertEquals(2, repository.findAll().size()),
                () -> assertExpenseEquals(
                        first,
                        repository.findById(first.getId()).orElseThrow()
                ),
                () -> assertExpenseEquals(
                        second,
                        repository.findById(second.getId()).orElseThrow()
                )
        );
    }

    @Test
    void saveShouldUpdateExpenseWithoutCreatingDuplicate() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        expense.setAmount(new BigDecimal("250.50"));
        expense.setDescription("Покупка продуктов");
        expense.setDate(LocalDate.of(2020, 2, 1));
        expense.setMerchant("Другой магазин");

        repository.save(expense);

        assertEquals(1, repository.findAll().size());
        assertExpenseEquals(
                expense,
                repository.findById(expense.getId()).orElseThrow()
        );
    }

    @Test
    void saveShouldPreserveCommasQuotesAndCyrillic() {
        Expense expected = createExpense(
                "Хлеб, молоко",
                "Магазин \"Продукты\", №5"
        );

        repository.save(expected);

        assertExpenseEquals(
                expected,
                repository.findById(expected.getId()).orElseThrow()
        );
    }

    @Test
    void saveShouldPreserveNullMerchant() {
        Expense expense = createExpense("Проезд", null);

        repository.save(expense);

        Expense restored = repository.findById(expense.getId()).orElseThrow();

        assertNull(restored.getMerchant());
    }

    @Test
    void saveShouldRejectNull() {
        assertThrows(
                NullPointerException.class,
                () -> repository.save(null)
        );
    }

    @Test
    void changingSavedObjectShouldNotChangeStoredExpense() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        expense.setAmount(new BigDecimal("999.99"));

        Expense stored = repository.findById(expense.getId()).orElseThrow();

        assertEquals(new BigDecimal("150.50"), stored.getAmount());
    }

    // FIND BY ID

    @Test
    void findByIdShouldReturnEmptyForUnknownId() {
        repository.save(createExpense("Продукты", "Магазин"));

        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByIdShouldReturnEmptyWhenFileIsMissing() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByIdShouldRejectNull() {
        assertThrows(
                NullPointerException.class,
                () -> repository.findById(null)
        );
    }

    @Test
    void changingFoundExpenseShouldNotChangeStorage() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        Expense found = repository.findById(expense.getId()).orElseThrow();
        found.setMerchant("Другой магазин");

        Expense stored = repository.findById(expense.getId()).orElseThrow();

        assertEquals("Магазин", stored.getMerchant());
    }

    // FIND ALL

    @Test
    void findAllShouldReturnEmptyWhenFileIsMissing() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void changingReturnedExpensesShouldNotChangeStorage() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        List<Expense> found = repository.findAll();
        found.getFirst().setAmount(new BigDecimal("999.99"));

        Expense stored = repository.findById(expense.getId()).orElseThrow();

        assertEquals(new BigDecimal("150.50"), stored.getAmount());
    }

    @Test
    void clearingReturnedListShouldNotChangeStorage() {
        repository.save(createExpense("Продукты", "Магазин"));

        List<Expense> found = repository.findAll();
        found.clear();

        assertEquals(1, repository.findAll().size());
    }

    @Test
    void findAllShouldRejectEmptyFile() throws IOException {
        Files.createFile(storageFile());

        assertThrows(
                ExpenseRepositoryException.class,
                () -> repository.findAll()
        );
    }

    @Test
    void findAllShouldRejectUnexpectedHeader() throws IOException {
        writeCsv("wrong,header\r\n");

        assertThrows(
                ExpenseRepositoryException.class,
                () -> repository.findAll()
        );
    }

    @Test
    void findAllShouldRejectInvalidExpense() throws IOException {
        String content = """
                id,created_at,description,amount,date,merchant
                123e4567-e89b-12d3-a456-426614174000,2020-01-02T10:00:00Z,Продукты,0,2020-01-01,Магазин
                """;

        writeCsv(content);

        assertThrows(
                ExpenseRepositoryException.class,
                () -> repository.findAll()
        );
    }

    @Test
    void findAllShouldRejectDuplicateIds() throws IOException {
        String header = "id,created_at,description,amount,date,merchant\r\n";
        String record =
                "123e4567-e89b-12d3-a456-426614174000,"
                        + "2020-01-02T10:00:00Z,"
                        + "Продукты,150.50,2020-01-01,Магазин\r\n";

        writeCsv(header + record + record);

        assertThrows(
                ExpenseRepositoryException.class,
                () -> repository.findAll()
        );
    }

    // DELETE BY ID

    @Test
    void deleteByIdShouldRemoveOnlyMatchingExpense() {
        Expense first = createExpense("Продукты", "Магазин");
        Expense second = createExpense("Проезд", null);

        repository.save(first);
        repository.save(second);

        assertTrue(repository.deleteById(first.getId()));

        CsvExpenseRepository reopened = new CsvExpenseRepository(directory);

        assertAll(
                () -> assertTrue(reopened.findById(first.getId()).isEmpty()),
                () -> assertEquals(1, reopened.findAll().size()),
                () -> assertExpenseEquals(
                        second,
                        reopened.findById(second.getId()).orElseThrow()
                )
        );
    }

    @Test
    void deleteByIdShouldReturnFalseForUnknownId() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        assertFalse(repository.deleteById(UUID.randomUUID()));

        assertExpenseEquals(
                expense,
                repository.findById(expense.getId()).orElseThrow()
        );
    }

    @Test
    void deleteByIdShouldReturnFalseWhenFileIsMissing() {
        assertFalse(repository.deleteById(UUID.randomUUID()));
    }

    @Test
    void deleteByIdShouldLeaveReadableStorageAfterDeletingLastExpense() {
        Expense expense = createExpense("Продукты", "Магазин");
        repository.save(expense);

        assertTrue(repository.deleteById(expense.getId()));

        CsvExpenseRepository reopened = new CsvExpenseRepository(directory);

        assertTrue(reopened.findAll().isEmpty());
    }

    @Test
    void deleteByIdShouldRejectNull() {
        assertThrows(
                NullPointerException.class,
                () -> repository.deleteById(null)
        );
    }

    // PROTECTION AGAINST DATA LOSS

    @Test
    void saveShouldNotOverwriteCorruptedFile() throws IOException {
        String original = "wrong,header\r\n";
        writeCsv(original);

        Expense expense = createExpense("Продукты", "Магазин");

        assertThrows(
                ExpenseRepositoryException.class,
                () -> repository.save(expense)
        );

        assertEquals(
                original,
                Files.readString(storageFile(), StandardCharsets.UTF_8)
        );
    }

    // SHARED HELPERS

    private static Expense createExpense(String description, String merchant) {
        return Expense.reconstitute(
                UUID.randomUUID(),
                Instant.parse("2020-01-02T10:00:00Z"),
                description,
                new BigDecimal("150.50"),
                LocalDate.of(2020, 1, 1),
                merchant
        );
    }

    private static void assertExpenseEquals(Expense expected, Expense actual) {
        assertAll(
                () -> assertEquals(expected.getId(), actual.getId()),
                () -> assertEquals(expected.getCreatedAt(), actual.getCreatedAt()),
                () -> assertEquals(expected.getDescription(), actual.getDescription()),
                () -> assertEquals(expected.getAmount(), actual.getAmount()),
                () -> assertEquals(expected.getDate(), actual.getDate()),
                () -> assertEquals(expected.getMerchant(), actual.getMerchant())
        );
    }

    private Path storageFile() {
        return directory.resolve("expenses.csv");
    }

    private void writeCsv(String content) throws IOException {
        Files.writeString(storageFile(), content, StandardCharsets.UTF_8);
    }
}