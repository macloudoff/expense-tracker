package model;

import config.AppConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Расход с неизменяемыми идентификатором и моментом создания записи.
 *
 * <p>Дата расхода задаётся отдельно от момента создания записи и не может
 * быть позже текущей даты в часовом поясе {@link AppConfig#TIME_ZONE}.
 *
 * <p>Описание обязательно: после нормализации содержит от 3 до 60 единиц
 * UTF-16, от 1 до 5 слов и хотя бы одну букву или цифру. Название торговой
 * точки необязательно: пустой ввод превращается в {@code null}; указанное
 * название содержит не более 60 единиц UTF-16 и хотя бы одну букву или цифру.
 * В обоих полях пробельные последовательности заменяются одним пробелом,
 * а пробелы по краям удаляются. Регистр и пунктуация сохраняются.
 *
 * <p>Сумма обязательна, строго положительна, не превышает 9999999.99
 * и имеет {@link BigDecimal#scale()} не более 2. Больший scale отклоняется
 * даже при завершающих нулях. Округление не выполняется; валюта моделью
 * не задаётся.
 *
 * <p>Правила действуют при создании, восстановлении и изменении расхода.
 * При ошибке проверки сеттер сохраняет прежнее значение поля.
 */
public final class Expense {
    private final UUID id;
    private final Instant createdAt;

    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private String merchant;

    /**
     * Создаёт расход с новым UUID и текущим моментом создания записи.
     *
     * @param description описание; нормализуется перед проверкой
     * @param amount сумма без округления
     * @param date дата расхода
     * @param merchant торговая точка; допускается {@code null}
     * @throws IllegalArgumentException если данные нарушают правила модели
     */
    public Expense(
            String description,
            BigDecimal amount,
            LocalDate date,
            String merchant) {
        setDescription(description);
        setAmount(amount);
        setDate(date);
        setMerchant(merchant);

        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    private Expense(
            UUID id,
            Instant createdAt,
            String description,
            BigDecimal amount,
            LocalDate date,
            String merchant
    ) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);

        setDescription(description);
        setAmount(amount);
        setDate(date);
        setMerchant(merchant);
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

    public void setMerchant(String merchant) {
        String normalizedMerchant = normalizeMerchant(merchant);
        validateMerchant(normalizedMerchant);
        this.merchant = normalizedMerchant;
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

    public String getMerchant() {
        return merchant;
    }

    /**
     * Восстанавливает расход с сохранёнными идентификатором и моментом
     * создания, не генерируя новые значения.
     *
     * <p>Текстовые поля нормализуются; данные проверяются по текущим правилам
     * модели, включая проверку даты относительно сегодняшнего дня.
     *
     * @param id сохранённый идентификатор
     * @param createdAt сохранённый момент создания записи
     * @param description описание расхода
     * @param amount сумма расхода
     * @param date дата расхода
     * @param merchant торговая точка; допускается {@code null}
     * @return восстановленный расход
     * @throws NullPointerException если {@code id} или {@code createdAt} равен {@code null}
     * @throws IllegalArgumentException если остальные данные нарушают правила модели
     */
    public static Expense reconstitute(
            UUID id,
            Instant createdAt,
            String description,
            BigDecimal amount,
            LocalDate date,
            String merchant
    ) {
        return new Expense(
                id, createdAt, description, amount, date, merchant
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

        LocalDate today = LocalDate.now(AppConfig.TIME_ZONE);
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }
    }

    private static String normalizeMerchant(String merchant) {
        if (merchant == null) {
            return null;
        }

        String normalized = merchant.replaceAll("(?U)\\s+", " ").strip();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }

    private static final int MAX_MERCHANT_LENGTH = 60;

    private static void validateMerchant(String normalizedMerchant) {
        if (normalizedMerchant == null) {
            return;
        }

        if (normalizedMerchant.length() > MAX_MERCHANT_LENGTH) {
            throw new IllegalArgumentException("Merchant must be less than or equal to 60 characters");
        }

        if (!normalizedMerchant.matches(".*[\\p{L}\\p{N}].*")) {
            throw new IllegalArgumentException(
                    "Merchant must contain at least one letter or digit"
            );
        }
    }
}