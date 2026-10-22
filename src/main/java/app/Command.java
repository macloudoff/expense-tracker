package app;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

record Command(String name, Map<String, String> options) {

    static Command parse(String[] args) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("help")) {
            if (args.length > 1) {
                throw new IllegalArgumentException("Help does not accept options");
            }
            return new Command("help", Map.of());
        }

        Set<String> allowed = switch (args[0]) {
            case "add" -> Set.of("--description", "--amount", "--merchant", "--date");
            case "list" -> Set.of();
            case "summary" -> Set.of("--month");
            case "delete" -> Set.of("--id");
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        };

        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            String option = args[i];
            if (!allowed.contains(option)) {
                throw new IllegalArgumentException("Unknown option: " + option);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            if (options.putIfAbsent(option, args[i + 1]) != null) {
                throw new IllegalArgumentException("Duplicate option: " + option);
            }
        }

        Command command = new Command(args[0], Map.copyOf(options));
        if (command.name.equals("add")) {
            command.required("--description");
            command.required("--amount");
        } else if (command.name.equals("delete")) {
            command.required("--id");
        }
        return command;
    }

    String required(String option) {
        String value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return value;
    }
}
