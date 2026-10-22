package repository.csv;

import model.Expense;
import repository.ExpenseRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CsvExpenseRepository implements ExpenseRepository {

    private final ExpenseCsvFile storage;

    public CsvExpenseRepository(Path directory) {
        this.storage = new ExpenseCsvFile(directory);
    }

    @Override
    public void save(Expense expense) {
        Objects.requireNonNull(expense, "Expense must not be null");

        List<Expense> expenses = new ArrayList<>(storage.read());

        expenses.removeIf(
                existing -> existing.getId().equals(expense.getId())
        );

        expenses.add(expense);
        storage.write(expenses);
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        return storage.read().stream()
                .filter(expense -> expense.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Expense> findAll() {
        return storage.read();
    }

    @Override
    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        List<Expense> expenses = new ArrayList<>(storage.read());

        boolean removed = expenses.removeIf(
                expense -> expense.getId().equals(id)
        );

        if (removed) {
            storage.write(expenses);
        }

        return removed;
    }
}