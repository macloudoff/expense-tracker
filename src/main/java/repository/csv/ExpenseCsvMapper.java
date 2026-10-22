package repository.csv;

import model.Expense;
import org.apache.commons.csv.CSVRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class ExpenseCsvMapper {

    private static final String ID = "id";
    private static final String CREATED_AT = "created_at";
    private static final String DESCRIPTION = "description";
    private static final String AMOUNT = "amount";
    private static final String DATE = "date";
    private static final String MERCHANT = "merchant";

    static final List<String> HEADER = List.of(
            ID,
            CREATED_AT,
            DESCRIPTION,
            AMOUNT,
            DATE,
            MERCHANT
    );

    private ExpenseCsvMapper() {
    }

    static Expense fromRecord(CSVRecord record) {
        if (record.size() != HEADER.size()) {
            throw new IllegalArgumentException(String.format(
                    "Expected %d columns, got %d",
                    HEADER.size(),
                    record.size()
            ));
        }

        String merchant = record.get(MERCHANT);

        return Expense.reconstitute(
                UUID.fromString(record.get(ID)),
                Instant.parse(record.get(CREATED_AT)),
                record.get(DESCRIPTION),
                new BigDecimal(record.get(AMOUNT)),
                LocalDate.parse(record.get(DATE)),
                merchant.isEmpty() ? null : merchant
        );
    }

    static List<String> toRecord(Expense expense) {
        return List.of(
                expense.getId().toString(),
                expense.getCreatedAt().toString(),
                expense.getDescription(),
                expense.getAmount().toPlainString(),
                expense.getDate().toString(),
                expense.getMerchant() == null ? "" : expense.getMerchant()
        );
    }
}