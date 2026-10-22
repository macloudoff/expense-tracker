package app;

import config.AppConfig;
import model.Expense;
import repository.ExpenseRepository;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ExpenseCli {

    static final String HELP = """
            Usage:
              expense-tracker add --description "Lunch" --amount 20 [--date YYYY-MM-DD] [--merchant "Cafe"]
              expense-tracker list
              expense-tracker summary [--month 1-12]
              expense-tracker delete --id NUMBER
              expense-tracker --help

            Dates default to today in the application timezone.
            Monthly summaries use the current year in that timezone.
            Storage: EXPENSE_TRACKER_DIR, or ./data by default.
            """;

    private final ExpenseRepository repository;
    private final PrintStream output;

    ExpenseCli(ExpenseRepository repository, PrintStream output) {
        this.repository = repository;
        this.output = output;
    }

    void execute(Command command) {
        switch (command.name()) {
            case "add" -> add(command);
            case "list" -> list();
            case "summary" -> summary(command);
            case "delete" -> delete(command);
            default -> throw new IllegalArgumentException("Unknown command: " + command.name());
        }
    }

    private void add(Command command) {
        String dateOption = command.options().get("--date");
        LocalDate date = dateOption == null
                ? LocalDate.now(AppConfig.TIME_ZONE)
                : LocalDate.parse(dateOption);
        Expense expense = new Expense(
                command.required("--description"),
                new BigDecimal(command.required("--amount")),
                date,
                command.options().get("--merchant")
        );
        Expense saved = repository.save(expense);
        output.printf("Expense added successfully (ID: %s)%n", saved.getId());
    }

    private void list() {
        List<Expense> expenses = repository.findAll();
        if (expenses.isEmpty()) {
            output.println("No expenses found.");
            return;
        }
        int descriptionWidth = Math.max("Description".length(), expenses.stream()
                .mapToInt(expense -> expense.getDescription().length())
                .max().orElse(0));
        int idWidth = Math.max(2, expenses.stream()
                .mapToInt(expense -> expense.getId().toString().length()).max().orElse(0));
        String format = "%-" + idWidth + "s  %-10s  %-" + descriptionWidth + "s  %s%n";
        output.printf(format, "ID", "Date", "Description", "Amount");
        expenses.stream()
                .sorted(Comparator.comparing(Expense::getDate)
                        .thenComparing(Expense::getCreatedAt)
                        .thenComparing(Expense::getId))
                .forEach(expense -> output.printf(
                        format,
                        expense.getId(),
                        expense.getDate(),
                        expense.getDescription(),
                        "$" + expense.getAmount().toPlainString()
                ));
    }

    private void summary(Command command) {
        String monthOption = command.options().get("--month");
        YearMonth period = null;
        if (monthOption != null) {
            Month month = Month.of(Integer.parseInt(monthOption));
            period = YearMonth.of(LocalDate.now(AppConfig.TIME_ZONE).getYear(), month);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : repository.findAll()) {
            if (period == null || YearMonth.from(expense.getDate()).equals(period)) {
                total = total.add(expense.getAmount());
            }
        }
        if (period == null) {
            output.printf("Total expenses: $%s%n", total.toPlainString());
        } else {
            String month = period.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            output.printf("Total expenses for %s: $%s%n", month, total.toPlainString());
        }
    }

    private void delete(Command command) {
        String value = command.required("--id");
        long id = Long.parseLong(value);
        if (id <= 0) {
            throw new IllegalArgumentException("Expected a positive number for --id");
        }
        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("Expense not found: " + id);
        }
        output.println("Expense deleted successfully");
    }
}
