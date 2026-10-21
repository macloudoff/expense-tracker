package model;

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
                null
        );
    }

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
}