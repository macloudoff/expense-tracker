package repository.csv;

import model.Expense;
import repository.ExpenseRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CsvExpenseRepository implements ExpenseRepository {

    private final ExpenseCsvFile storage;
    private final ExpenseIdSequence sequence;

    public CsvExpenseRepository(Path directory) {
        this.storage = new ExpenseCsvFile(directory);
        this.sequence = new ExpenseIdSequence(directory);
    }

    @Override
    public Expense save(Expense expense) {
        Objects.requireNonNull(expense, "Expense must not be null");

        List<Expense> expenses = new ArrayList<>(readExpenses());

        long id = expense.getId() == null ? sequence.next() : expense.getId();
        sequence.ensureAtLeast(id);
        Expense saved = Expense.reconstitute(
                id, expense.getCreatedAt(), expense.getDescription(),
                expense.getAmount(), expense.getDate(), expense.getMerchant()
        );
        expenses.removeIf(existing -> existing.getId().equals(id));
        expenses.add(saved);
        storage.write(expenses);
        return saved;
    }

    @Override
    public Optional<Expense> findById(Long id) {
        Objects.requireNonNull(id, "Id must not be null");
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        return readExpenses().stream()
                .filter(expense -> expense.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Expense> findAll() {
        return readExpenses();
    }

    private List<Expense> readExpenses() {
        List<Expense> expenses = storage.read();
        long maximum = expenses.stream().mapToLong(Expense::getId).max().orElse(0);
        sequence.ensureAtLeast(maximum);
        return expenses;
    }

    @Override
    public boolean deleteById(Long id) {
        Objects.requireNonNull(id, "Id must not be null");
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        List<Expense> expenses = new ArrayList<>(readExpenses());

        boolean removed = expenses.removeIf(
                expense -> expense.getId().equals(id)
        );

        if (removed) {
            storage.write(expenses);
        }

        return removed;
    }
}