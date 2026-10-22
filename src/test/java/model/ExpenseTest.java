package model;

import config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseTest {

    private Expense validExpense;

    @BeforeEach
    void createValidExpense() {
        this.validExpense = new Expense(
                "test",
                new BigDecimal("1"),
                LocalDate.of(2020, 1, 1),
                "test"
        );
    }

    // SET DESCRIPTION

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "\t",
            "\n",
            "---",
            "A",
            "AB",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789",
            "one two three four five six"
    })
    void setDescriptionRejectsInvalidValues(String value) {
        String previous = validExpense.getDescription();
        assertThrows(IllegalArgumentException.class, () -> validExpense.setDescription(value));
        assertEquals(previous, validExpense.getDescription());
    }

    @ParameterizedTest
    @CsvSource({
            "'123', '123'",
            "'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz12345678', 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz12345678'",
            "'1  2  3', '1 2 3'",
            "'1  2  3  4', '1 2 3 4'",
            "'1      2', '1 2'",
            "'  test  ', 'test'",
            "'1 2 3 4 5', '1 2 3 4 5'",
            "'1\t2', '1 2'",
            "'Продукты', 'Продукты'",
            "'Магазин №5!', 'Магазин №5!'"
    })
    void setDescriptionAcceptsValidValues(String value, String expected) {
        validExpense.setDescription(value);
        assertEquals(expected, validExpense.getDescription());
    }

    @Test
    void setDescriptionNormalizesLineBreaks() {
        validExpense.setDescription("1\n2");
        assertEquals("1 2", validExpense.getDescription());
    }

    @Test
    void setDescriptionValidatesLengthAfterNormalization() {
        String expected = "A".repeat(60);
        validExpense.setDescription("  " + expected + "  ");
        assertEquals(expected, validExpense.getDescription());
    }

    // SET AMOUNT

    @ParameterizedTest
    @ValueSource(strings = {
            "0",
            "-1",
            "10000000.00",
            "10000000",
            "1.000",
            "1.256"
    })
    void setAmountRejectsInvalidValues(String values) {
        BigDecimal previous = validExpense.getAmount();
        BigDecimal current = new BigDecimal(values);
        assertThrows(IllegalArgumentException.class, () -> validExpense.setAmount(current));
        assertEquals(previous, validExpense.getAmount());
    }

    @Test
    void setAmountShouldRejectNullValue() {
        BigDecimal previous = validExpense.getAmount();
        assertThrows(IllegalArgumentException.class, () -> validExpense.setAmount(null));
        assertEquals(previous, validExpense.getAmount());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "9999999.99",
            "1",
            "0.1",
            "0.11"
    })
    void setAmountShouldAcceptValidValues(String value) {
        BigDecimal amount = new BigDecimal(value);
        validExpense.setAmount(amount);
        assertEquals(amount, validExpense.getAmount());
    }

    // SET DATE

    @Test
    void setDateShouldRejectTomorrow() {
        LocalDate previous = validExpense.getDate();
        LocalDate tomorrow = LocalDate.now(AppConfig.TIME_ZONE).plusDays(1);
        assertThrows(IllegalArgumentException.class, () -> validExpense.setDate(tomorrow));
        assertEquals(previous, validExpense.getDate());
    }

    @Test
    void setDateShouldRejectNull() {
        LocalDate previous = validExpense.getDate();
        assertThrows(IllegalArgumentException.class, () -> validExpense.setDate(null));
        assertEquals(previous, validExpense.getDate());
    }

    @Test
    void setDateShouldAcceptToday() {
        LocalDate now = LocalDate.now(AppConfig.TIME_ZONE);
        validExpense.setDate(now);
        assertEquals(now, validExpense.getDate());
    }

    @Test
    void setDateShouldAcceptPast() {
        LocalDate past = LocalDate.now(AppConfig.TIME_ZONE).minusYears(10);
        validExpense.setDate(past);
        assertEquals(past, validExpense.getDate());
    }

    //SET MERCHANT

    @ParameterizedTest
    @ValueSource(strings = {
            "---",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456780"
    })
    void setMerchantShouldRejectInvalidValue(String value) {
        String previous = validExpense.getMerchant();
        assertThrows(IllegalArgumentException.class, () -> validExpense.setMerchant(value));
        assertEquals(previous, validExpense.getMerchant());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DFDSFSD",
            "fdsfdsf, fdsfsdf, dfsdfs",
            "1",
            "a",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz12345678"
    })
    void setMerchantShouldAcceptValidValue(String value) {
        validExpense.setMerchant(value);
        assertEquals(value, validExpense.getMerchant());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "    ",
            "\t",
            "\n"
    })
    void setMerchantShouldAcceptNullAndEmpty(String value) {
        validExpense.setMerchant(value);
        assertNull(validExpense.getMerchant());
    }

    @ParameterizedTest
    @CsvSource({
            "'   text   ', 'text'",
            "'Магазин   №5', 'Магазин №5'",
            "'Магазин\t№5', 'Магазин №5'"
    })
    void setMerchantShouldNormalizeWhitespace(String value, String expected) {
        validExpense.setMerchant(value);
        assertEquals(expected, validExpense.getMerchant());
    }

    @Test
    void setMerchantShouldValidateLengthAfterNormalization() {
        String expected = "A".repeat(60);
        validExpense.setMerchant("  " + expected + "  ");
        assertEquals(expected, validExpense.getMerchant());
    }

    @Test
    void setMerchantShouldNormalizeLineBreaks() {
        validExpense.setMerchant("Магазин\n№5");
        assertEquals("Магазин №5", validExpense.getMerchant());
    }

    // Constructor
    @Test
    void constructorShouldCreateExpense() {
        BigDecimal amount = new BigDecimal("150.50");
        LocalDate date = LocalDate.of(2020, 1, 1);

        Expense expense = new Expense(
                "  Покупка   продуктов  ",
                amount,
                date,
                "  Магазин   №5  "
        );

        assertAll(
                () -> assertNull(expense.getId()),
                () -> assertNotNull(expense.getCreatedAt()),
                () -> assertEquals("Покупка продуктов", expense.getDescription()),
                () -> assertEquals(amount, expense.getAmount()),
                () -> assertEquals(date, expense.getDate()),
                () -> assertEquals("Магазин №5", expense.getMerchant())
        );
    }

    @Test
    void constructorShouldAcceptNullMerchant() {
        Expense expense = new Expense(
                "Продукты",
                new BigDecimal("150.50"),
                LocalDate.of(2020, 1, 1),
                null
        );

        assertNull(expense.getMerchant());
    }

    @ParameterizedTest
    @MethodSource("invalidExpenseData")
    void constructorShouldRejectInvalidData(
            String description,
            BigDecimal amount,
            LocalDate date,
            String merchant
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Expense(description, amount, date, merchant)
        );
    }

    @Test
    void reconstituteShouldRestoreExpense() {
        Long id = 1L;
        Instant createdAt = Instant.parse("2020-01-02T10:00:00Z");
        BigDecimal amount = new BigDecimal("150.50");
        LocalDate date = LocalDate.of(2020, 1, 1);

        Expense expense = Expense.reconstitute(
                id,
                createdAt,
                "  Покупка   продуктов  ",
                amount,
                date,
                "  Магазин   №5  "
        );

        assertAll(
                () -> assertEquals(id, expense.getId()),
                () -> assertEquals(createdAt, expense.getCreatedAt()),
                () -> assertEquals("Покупка продуктов", expense.getDescription()),
                () -> assertEquals(amount, expense.getAmount()),
                () -> assertEquals(date, expense.getDate()),
                () -> assertEquals("Магазин №5", expense.getMerchant())
        );
    }

    @Test
    void reconstituteShouldAcceptNullMerchant() {
        Expense expense = Expense.reconstitute(
                1L,
                Instant.parse("2020-01-02T10:00:00Z"),
                "Продукты",
                new BigDecimal("150.50"),
                LocalDate.of(2020, 1, 1),
                null
        );

        assertNull(expense.getMerchant());
    }

    @Test
    void reconstituteShouldRejectNullId() {
        assertThrows(
                NullPointerException.class,
                () -> Expense.reconstitute(
                        null,
                        Instant.parse("2020-01-02T10:00:00Z"),
                        "Продукты",
                        new BigDecimal("150.50"),
                        LocalDate.of(2020, 1, 1),
                        "Магазин"
                )
        );
    }

    @Test
    void reconstituteShouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> Expense.reconstitute(
                        1L,
                        null,
                        "Продукты",
                        new BigDecimal("150.50"),
                        LocalDate.of(2020, 1, 1),
                        "Магазин"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidExpenseData")
    void reconstituteShouldRejectInvalidData(
            String description,
            BigDecimal amount,
            LocalDate date,
            String merchant
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Expense.reconstitute(
                        1L,
                        Instant.parse("2020-01-02T10:00:00Z"),
                        description,
                        amount,
                        date,
                        merchant
                )
        );
    }

    private static Stream<Arguments> invalidExpenseData() {
        BigDecimal amount = new BigDecimal("150.50");
        LocalDate date = LocalDate.of(2020, 1, 1);

        return Stream.of(
                Arguments.of("---", amount, date, "Магазин"),
                Arguments.of("Продукты", BigDecimal.ZERO, date, "Магазин"),
                Arguments.of("Продукты", amount, null, "Магазин"),
                Arguments.of("Продукты", amount, date, "---")
        );
    }
}