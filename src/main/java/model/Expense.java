package model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Expense {
    private final UUID id;
    private final Instant createdAt;

    private String description;
    private BigDecimal amount;

    private LocalDate date;


    public Expense(String description, BigDecimal amount, LocalDate date) {
        setDescription(description);
        setAmount(amount);
        setDate(date);

        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    private Expense(
            UUID id,
            Instant createdAt,
            String description,
            BigDecimal amount,
            LocalDate date
    ) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);

        setDescription(description);
        setAmount(amount);
        setDate(date);
    }

    public void setDescription(String description) {
        String normalizedDescription = normalizeDescription(description);
        validateDescription(normalizedDescription);
        this.description = normalizedDescription;
    }

    public void setAmount(BigDecimal amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    public void setDate(LocalDate date) {
        validateDate(date);
        this.date = date;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public static Expense reconstitute(
            UUID id,
            Instant createdAt,
            String description,
            BigDecimal amount,
            LocalDate date
    ) {
        return new Expense(
                id, createdAt, description, amount, date
        );
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }

        return description.replaceAll("(?U)\\s+", " ").strip();
    }

    private static final int MIN_DESCRIPTION_LENGTH = 3;
    private static final int MAX_DESCRIPTION_LENGTH = 60;
    private static final int MIN_DESCRIPTION_WORDS = 1;
    private static final int MAX_DESCRIPTION_WORDS = 5;

    private static void validateDescription(String normalizedDescription) {
        int len = normalizedDescription.length();
        if (len < MIN_DESCRIPTION_LENGTH || len > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Description must be between 3 and 60 characters"
            );
        }

        int words = normalizedDescription.split(" ").length;
        if (words < MIN_DESCRIPTION_WORDS || words > MAX_DESCRIPTION_WORDS) {
            throw new IllegalArgumentException(
                    "Description must contain between 1 and 5 words"
            );
        }

        boolean containsLetterOrDigit = normalizedDescription.matches(
                ".*[\\p{L}\\p{N}].*"
        );

        if (!containsLetterOrDigit) {
            throw new IllegalArgumentException(
                    "Description must contain at least one letter or digit"
            );
        }
    }

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999.99");

    private static void validateAmount(final BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most 2 decimal places");
        }

        if (amount.signum() != 1) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount must be less than or equal to 9999999.99");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }
    }
}