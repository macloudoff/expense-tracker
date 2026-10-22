package app;

import config.AppConfig;
import model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.csv.CsvExpenseRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseCliTest {

    @TempDir
    Path directory;

    private CsvExpenseRepository repository;
    private ExpenseCli cli;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        repository = new CsvExpenseRepository(directory);
        output = new ByteArrayOutputStream();
        cli = new ExpenseCli(repository, new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @Test
    void shouldAddListSummarizeAndDeletePersistedExpenses() {
        run("add", "--description", "Lunch", "--amount", "20");
        run("add", "--description", "Dinner", "--amount", "10");

        Expense dinner = repository.findAll().stream()
                .filter(expense -> expense.getDescription().equals("Dinner"))
                .findFirst().orElseThrow();
        assertEquals(2L, dinner.getId());
        assertTrue(text().contains("Expense added successfully (ID: 1)"));
        assertTrue(text().contains("Expense added successfully (ID: " + dinner.getId() + ")"));

        output.reset();
        run("list");
        assertAll(
                () -> assertTrue(text().contains("Description")),
                () -> assertTrue(text().contains("Lunch")),
                () -> assertTrue(text().contains("Dinner")),
                () -> assertTrue(text().contains(dinner.getId().toString()))
        );

        output.reset();
        run("summary");
        assertEquals("Total expenses: $30", text().strip());

        output.reset();
        run("delete", "--id", dinner.getId().toString());
        assertEquals("Expense deleted successfully", text().strip());
        assertTrue(new CsvExpenseRepository(directory).findById(dinner.getId()).isEmpty());

        output.reset();
        run("summary");
        assertEquals("Total expenses: $20", text().strip());
    }

    @Test
    void monthlySummaryShouldExcludeOtherMonthsAndYears() {
        int year = LocalDate.now(AppConfig.TIME_ZONE).getYear();
        saveExpense(LocalDate.of(year, 1, 1), "20");
        saveExpense(LocalDate.of(year - 1, 1, 1), "100");
        saveExpense(LocalDate.of(year - 1, 12, 1), "200");

        run("summary", "--month", "1");

        assertEquals("Total expenses for January: $20", text().strip());
    }

    @Test
    void shouldHandleEmptyStorage() {
        run("list");
        assertEquals("No expenses found.", text().strip());
        output.reset();
        run("summary");
        assertEquals("Total expenses: $0", text().strip());
    }

    @Test
    void shouldRejectInvalidAmountWithoutSaving() {
        assertThrows(IllegalArgumentException.class,
                () -> run("add", "--description", "Lunch", "--amount", "0"));
        assertTrue(repository.findAll().isEmpty());
        assertEquals("", text());
    }

    @Test
    void shouldAcceptExplicitDateAndMerchant() {
        run("add", "--description", "Lunch", "--amount", "20.50",
                "--date", "2020-01-02", "--merchant", "Cafe");
        Expense saved = repository.findAll().getFirst();
        assertAll(
                () -> assertEquals(LocalDate.of(2020, 1, 2), saved.getDate()),
                () -> assertEquals("Cafe", saved.getMerchant()),
                () -> assertEquals(new BigDecimal("20.50"), saved.getAmount())
        );
    }

    @Test
    void shouldRejectInvalidAndUnknownIds() {
        assertThrows(IllegalArgumentException.class, () -> run("delete", "--id", "not-an-id"));
        assertThrows(IllegalArgumentException.class,
                () -> run("delete", "--id", "999"));
    }

    @Test
    void parserShouldRejectMissingDuplicateAndUnknownOptions() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Command.parse(new String[]{"add", "--description", "Lunch"})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Command.parse(new String[]{"summary", "--month"})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Command.parse(new String[]{"summary", "--month", "1", "--month", "2"})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Command.parse(new String[]{"list", "--unknown", "value"}))
        );
    }

    private void run(String... args) {
        cli.execute(Command.parse(args));
    }

    private String text() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private void saveExpense(LocalDate date, String amount) {
        repository.save(Expense.reconstitute(
                repository.findAll().size() + 1L, Instant.parse("2020-01-01T00:00:00Z"),
                "Expense", new BigDecimal(amount), date, null
        ));
    }
}
