// PTUI.java
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Scanner;
import java.util.List;
import java.util.Set;

public class PTUI {

    private final Controller controller;
    private final Scanner scanner;

    public PTUI() {
        controller = new Controller();
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        PTUI ptui = new PTUI();
        ptui.run();
    }

    private void printMenu() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("ADD      - Add a new coin");
        System.out.println("LIST     - List all coins");
        System.out.println("SEARCH   - Search coins by attribute");
        System.out.println("EDIT     - Edit a coin");
        System.out.println("DELETE   - Delete a coin");
        System.out.println("HELP     - Show this menu");
        System.out.println("EXIT     - Exit");
        System.out.println("==========================");
    }

    private void run() {
        printMenu();
        while (true) {
            System.out.print("> ");
            String choice = scanner.nextLine().trim();

            switch (choice.toUpperCase()) {
                case "ADD":
                    addCoin();
                    break;
                case "LIST":
                    listAllCoins();
                    break;
                case "SEARCH":
                    searchCoins();
                    break;
                case "EDIT":
                    editCoin();
                    break;
                case "DELETE":
                    deleteCoin();
                    break;
                case "HELP":
                    printMenu();
                    break;
                case "EXIT":
                    System.out.println("Exiting. Goodbye!");
                    return;
                default:
                    // Invalid command: re-prompt
                    break;
            }
        }
    }

    private void addCoin() {
        while (true) {
            System.out.print("Enter coin name (or 'back'): ");
            String name = scanner.nextLine().trim();
            if (name.equalsIgnoreCase("back")) return;
            if (!name.isEmpty()) {
                // Build a minimal rawFields map for createCoin
                Map<String, String> raw = new HashMap<>();
                raw.put("name", name);
                raw.put("date", "0");      // default date
                raw.put("grade", "N/A");   // default grade
                // other fields can be left blank or absent

                Controller.ValidationResult vr = controller.createCoin(raw);
                if (!vr.isValid()) {
                    // This should not happen since date="0" and grade="N/A" are valid,
                    // but we print any unexpected errors:
                    for (Controller.FieldError fe : vr.getErrors()) {
                        System.out.printf("Error: invalid '%s' (expected %s)%n",
                            fe.getField(), fe.getExpectedType());
                    }
                } else {
                    UUID id = vr.getCreatedId();
                    System.out.println("Coin added with ID: " + id);
                    return;
                }
            }
            // Otherwise re-prompt
        }
    }

    private void listAllCoins() {
        List<Coin> coins = controller.listCoins();
        System.out.println("\nAll Coins (by name):");
        if (coins.isEmpty()) {
            System.out.println("  [No coins in the database]");
        } else {
            for (int i = 0; i < coins.size(); i++) {
                Coin c = coins.get(i);
                System.out.printf("  %d. %s (ID: %s)%n", i + 1, c.getName(), c.getId());
            }
        }
        System.out.println();  // blank line

        // Prompt until valid input
        while (true) {
            System.out.print("Type 'back' to return or 'view' to view a coin: ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) {
                return;
            } else if (input.equalsIgnoreCase("view")) {
                // View loop
                while (true) {
                    System.out.print("Enter coin number to view (or 'back'): ");
                    String idxStr = scanner.nextLine().trim();
                    if (idxStr.equalsIgnoreCase("back")) {
                        return;
                    }
                    try {
                        int index = Integer.parseInt(idxStr) - 1;
                        if (index < 0 || index >= coins.size()) {
                            System.out.println("Invalid input");
                            continue;
                        }
                        Coin selected = coins.get(index);
                        System.out.println("\nCoin Details:");
                        System.out.println(selected.toString());
                        System.out.println(); // blank line
                        return;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input");
                    }
                }
            } else {
                System.out.println("Invalid input");
            }
        }
    }

    private void searchCoins() {
        while (true) {
            System.out.println();  // blank line before showing attributes
            System.out.println("Searchable attributes:");
            System.out.println("id");
            System.out.println("name");
            System.out.println("date");
            System.out.println("thickness");
            System.out.println("diameter");
            System.out.println("grade");
            System.out.println("composition");
            System.out.println("denomination");
            System.out.println("edge");
            System.out.println("weight");
            System.out.println();  // blank line before prompt
            System.out.print("Enter attribute to search by (or 'back'): ");
            String attr = scanner.nextLine().trim().toLowerCase();
            if (attr.equalsIgnoreCase("back")) return;

            Set<String> validAttrs = Set.of(
                    "id", "name", "date", "thickness", "diameter",
                    "grade", "composition", "denomination", "edge", "weight"
            );
            if (!validAttrs.contains(attr)) {
                System.out.println("Invalid input");
                continue;
            }

            while (true) {
                System.out.print("Enter value to search for (or 'back'): ");
                String value = scanner.nextLine().trim();
                if (value.equalsIgnoreCase("back")) return;

                List<Coin> matches = controller.searchCoins(attr, value);
                System.out.println("\nMatching Coins:");
                if (matches.isEmpty()) {
                    System.out.println("  [No coins match that attribute/value pair]");
                } else {
                    for (int i = 0; i < matches.size(); i++) {
                        Coin c = matches.get(i);
                        System.out.printf("  %d. %s (ID: %s)%n", i + 1, c.getName(), c.getId());
                    }
                }
                System.out.println();  // blank line
                // Prompt until valid input
                while (true) {
                    System.out.print("Type 'back' to return or 'view' to view a coin: ");
                    String input2 = scanner.nextLine().trim();
                    if (input2.equalsIgnoreCase("back")) {
                        return;
                    } else if (input2.equalsIgnoreCase("view")) {
                        while (true) {
                            System.out.print("Enter coin number to view (or 'back'): ");
                            String idxStr = scanner.nextLine().trim();
                            if (idxStr.equalsIgnoreCase("back")) {
                                return;
                            }
                            try {
                                int index = Integer.parseInt(idxStr) - 1;
                                List<Coin> matchesList = controller.searchCoins(attr, value);
                                if (index < 0 || index >= matchesList.size()) {
                                    System.out.println("Invalid input");
                                    continue;
                                }
                                Coin selected = matchesList.get(index);
                                System.out.println("\nCoin Details:");
                                System.out.println(selected.toString());
                                System.out.println(); // blank line
                                return;
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input");
                            }
                        }
                    } else {
                        System.out.println("Invalid input");
                    }
                }
                // End prompt loop
            }
        }
    }

    private void editCoin() {
        while (true) {
            System.out.print("Would you like to 'search' by attribute or 'list' all coins? (or 'back'): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            if (choice.equalsIgnoreCase("back")) return;

            List<Coin> candidates;
            if (choice.equals("search")) {
                candidates = performSearchForEdit();
                if (candidates == null) return;
                if (candidates.isEmpty()) continue;
            } else if (choice.equals("list")) {
                candidates = controller.listCoins();
                if (candidates.isEmpty()) continue;
            } else {
                continue;
            }

            while (true) {
                System.out.println("Select a coin by index (or 'back'):");
                for (int i = 0; i < candidates.size(); i++) {
                    Coin c = candidates.get(i);
                    System.out.printf("  %d) %s (ID: %s)%n", i + 1, c.getName(), c.getId());
                }
                System.out.print("> ");
                String idxStr = scanner.nextLine().trim();
                if (idxStr.equalsIgnoreCase("back")) return;

                int index;
                try {
                    index = Integer.parseInt(idxStr) - 1;
                    if (index < 0 || index >= candidates.size()) continue;
                } catch (NumberFormatException e) {
                    continue;
                }

                Coin coinToEdit = candidates.get(index);
                Set<String> editableAttrs = Set.of(
                        "name", "date", "thickness", "diameter", "grade",
                        "composition", "denomination", "edge", "weight"
                );

                while (true) {
                    System.out.println("Editable attributes: name, date, thickness, diameter, grade, composition, denomination, edge, weight");
                    System.out.print("Enter attribute to change (or 'back'): ");
                    String attr = scanner.nextLine().trim().toLowerCase();
                    if (attr.equalsIgnoreCase("back")) return;
                    if (!editableAttrs.contains(attr)) continue;

                    while (true) {
                        System.out.print("Enter new value for '" + attr + "' (or 'back'): ");
                        String newValue = scanner.nextLine().trim();
                        if (newValue.equalsIgnoreCase("back")) return;

                        boolean success = setAttribute(coinToEdit, attr, newValue);
                        if (success) {
                            controller.saveCoin(coinToEdit);
                            break;
                        }
                        // Otherwise re-prompt newValue
                    }
                }
            }
        }
    }

    private List<Coin> performSearchForEdit() {
        while (true) {
            System.out.println("Searchable attributes:");
            System.out.println("id");
            System.out.println("name");
            System.out.println("date");
            System.out.println("thickness");
            System.out.println("diameter");
            System.out.println("grade");
            System.out.println("composition");
            System.out.println("denomination");
            System.out.println("edge");
            System.out.println("weight");
            System.out.print("Enter attribute to search by (or 'back'): ");
            String attr = scanner.nextLine().trim().toLowerCase();
            if (attr.equalsIgnoreCase("back")) return null;

            Set<String> validAttrs = Set.of(
                    "id", "name", "date", "thickness", "diameter",
                    "grade", "composition", "denomination", "edge", "weight"
            );
            if (!validAttrs.contains(attr)) {
                System.out.println("Invalid input");
                continue;
            }

            while (true) {
                System.out.print("Enter value to search for (or 'back'): ");
                String value = scanner.nextLine().trim();
                if (value.equalsIgnoreCase("back")) return null;

                return controller.searchCoins(attr, value);
            }
        }
    }

    private boolean setAttribute(Coin c, String attr, String newValue) {
        try {
            switch (attr) {
                case "name":
                    if (!newValue.isEmpty()) {
                        c.setName(newValue);
                        return true;
                    }
                    return false;
                case "date":
                    c.setDate(Integer.parseInt(newValue));
                    return true;
                case "thickness":
                    c.setThickness(Double.parseDouble(newValue));
                    return true;
                case "diameter":
                    c.setDiameter(Double.parseDouble(newValue));
                    return true;
                case "grade":
                    if (!newValue.isEmpty()) {
                        c.setGrade(newValue);
                        return true;
                    }
                    return false;
                case "composition":
                    if (!newValue.isEmpty()) {
                        c.setComposition(newValue);
                        return true;
                    }
                    return false;
                case "denomination":
                    if (!newValue.isEmpty()) {
                        c.setDenomination(newValue);
                        return true;
                    }
                    return false;
                case "edge":
                    if (!newValue.isEmpty()) {
                        c.setEdge(newValue);
                        return true;
                    }
                    return false;
                case "weight":
                    c.setWeight(Double.parseDouble(newValue));
                    return true;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void deleteCoin() {
        while (true) {
            System.out.print("Would you like to 'search' by attribute or 'list' all coins? (or 'back'): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            if (choice.equalsIgnoreCase("back")) return;

            List<Coin> candidates;
            if (choice.equals("search")) {
                candidates = performSearchForEdit();
                if (candidates == null) return;
                if (candidates.isEmpty()) continue;
            } else if (choice.equals("list")) {
                candidates = controller.listCoins();
                if (candidates.isEmpty()) continue;
            } else {
                continue;
            }

            while (true) {
                System.out.println("Select a coin to delete by index (or 'back'):");
                for (int i = 0; i < candidates.size(); i++) {
                    Coin c = candidates.get(i);
                    System.out.printf("  %d) %s (ID: %s)%n", i + 1, c.getName(), c.getId());
                }
                System.out.print("> ");
                String idxStr = scanner.nextLine().trim();
                if (idxStr.equalsIgnoreCase("back")) return;

                int index;
                try {
                    index = Integer.parseInt(idxStr) - 1;
                    if (index < 0 || index >= candidates.size()) continue;
                } catch (NumberFormatException e) {
                    continue;
                }

                Coin coinToDelete = candidates.get(index);

                while (true) {
                    System.out.println();  // blank line
                    System.out.printf(
                            "Are you sure you want to delete '%s' (ID: %s)? (yes/no): ",
                            coinToDelete.getName(), coinToDelete.getId()
                    );
                    String confirm = scanner.nextLine().trim().toLowerCase();
                    if (confirm.equals("yes") || confirm.equals("y")) {
                        controller.deleteCoin(coinToDelete);
                        System.out.println("Coin deleted.");
                        return;
                    } else if (confirm.equals("no") || confirm.equals("n") || confirm.equalsIgnoreCase("back")) {
                        return;
                    }
                    // Otherwise re-prompt confirmation
                }
            }
        }
    }
}