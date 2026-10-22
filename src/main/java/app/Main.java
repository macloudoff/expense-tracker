package app;

import repository.ExpenseRepositoryException;
import repository.csv.CsvExpenseRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;

/** Точка входа CLI. Каталог хранения задаётся через EXPENSE_TRACKER_DIR. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int status = execute(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    private static int execute(String[] args) {
        try {
            Command command = Command.parse(args);
            if (command.name().equals("help")) {
                System.out.print(ExpenseCli.HELP);
                return 0;
            }

            String configuredDirectory = System.getenv("EXPENSE_TRACKER_DIR");
            Path directory = Path.of(configuredDirectory == null
                    ? "data" : configuredDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            ExpenseCli cli = new ExpenseCli(new CsvExpenseRepository(directory), System.out);
            cli.execute(command);
            return 0;
        } catch (IllegalArgumentException | DateTimeException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Run expense-tracker --help for usage.");
            return 2;
        } catch (IOException | ExpenseRepositoryException | SecurityException e) {
            System.err.println("Storage error: " + e.getMessage());
            return 1;
        }
    }
}
