package repository;

public final class ExpenseRepositoryException extends RuntimeException {
    public ExpenseRepositoryException(String message) {
        super(message);
    }

    public ExpenseRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
