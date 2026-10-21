package model;

import config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}