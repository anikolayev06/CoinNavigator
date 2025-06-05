import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Controller for CoinNavigator.
 *
 *  – Manages an “errorBox” (for the GUI to display validation/errors).
 *  – Delegates to Database for CRUD on multiple “lists.”
 *  – Exposes coin‐attribute metadata so the GUI can build forms/tables dynamically.
 *  – Remembers the last‐opened list across restarts via java.util.prefs.Preferences.
 */
public class Controller {

    private final Database db;

    // ─── “Error Box” for GUI ────────────────────────────────────────────────────────
    private final List<String> errorBox = new ArrayList<>();

    /** Return current error messages (plain text). */
    public List<String> getErrorBox() {
        return errorBox;
    }

    /** Clear any stored error messages. */
    public void clearErrorBox() {
        errorBox.clear();
    }

    // ─── Preferences for “lastOpenedList” ─────────────────────────────────────────
    // We store “lastOpenedList” under the key "lastList" in java.util.prefs.
    private static final Preferences PREFS = Preferences.userNodeForPackage(Controller.class);
    private static final String LAST_LIST_KEY = "lastList";

    /**
     * Return the name of the last‐opened list (as saved in Preferences).
     * If no value is saved, returns null.
     */
    public String getLastOpenedList() {
        return PREFS.get(LAST_LIST_KEY, null);
    }

    /**
     * Remember that `listName` was the last‐opened list. Written to Preferences immediately.
     */
    public void setLastOpenedList(String listName) {
        if (listName != null) {
            PREFS.put(LAST_LIST_KEY, listName);
        }
    }
    // ────────────────────────────────────────────────────────────────────────────────

    // ─── Constructor ───────────────────────────────────────────────────────────────
    public Controller() {
        this.db = new Database();
    }

    // ─── LIST MANAGEMENT ───────────────────────────────────────────────────────────

    /**
     * Return all saved list names (e.g. “Owned”, “Wishlist”, plus any custom ones).
     */
    public List<String> getAllListNames() {
        return db.getAllListNames();
    }

    /**
     * Delete a list with the given name unless it is a protected list ("Owned" or "Wishlist").
     *
     * Returns false if the list was protected (and an error was added), true if it was actually deleted.
     */
    public boolean deleteList(String listName) {
        if (listName == null) {
            return false;
        }
        if (listName.equalsIgnoreCase("Owned") || listName.equalsIgnoreCase("Wishlist")) {
            errorBox.add("Cannot delete Owned or Wishlist");
            return false;
        }
        db.deleteList(listName);
        return true;
    }

    /**
     * Called when the user clicks “+” to make a brand‐new list.
     * Persist it in metadata (and implicitly, a new table is created).
     */
    public void createList(String listName) {
        db.createList(listName);
    }

    // ─── COIN CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new Coin in the specified list/table (validates inputs).
     * If validation fails, `errorBox` is populated; required fields are: name, date, grade.
     *
     * @param listName  the name of the table into which this coin should be inserted
     * @param rawFields a map of raw‐string inputs keyed by attribute name, e.g.
     *                  "name", "date", "grade", "diameter", "thickness", "edge", "weight", "composition", "denomination"
     *
     * @return a ValidationResult: if valid, `getCreatedId()` is non‐null; otherwise
     *         `getErrors()` holds one or more FieldError objects and `errorBox` holds one or more plain‐text messages.
     */
    public ValidationResult createCoinInList(String listName, Map<String, String> rawFields) {
        clearErrorBox();
        ValidationResult result = new ValidationResult();

        // Pull all raw strings (may be empty):
        String name           = rawFields.getOrDefault("name", "").trim();
        String dateText       = rawFields.getOrDefault("date", "").trim();
        String gradeText      = rawFields.getOrDefault("grade", "").trim();

        // The rest come from Coin.getAttributeNamesInOrder(), but we are only parsing numerics below:
        String diameterText   = rawFields.getOrDefault("diameter", "").trim();
        String thicknessText  = rawFields.getOrDefault("thickness", "").trim();
        String edge           = rawFields.getOrDefault("edge", "").trim();
        String weightText     = rawFields.getOrDefault("weight", "").trim();
        String composition    = rawFields.getOrDefault("composition", "").trim();
        String denomination   = rawFields.getOrDefault("denomination", "").trim();

        // 1) Required: name, date, grade (all three must be nonempty)
        if (name.isEmpty() || dateText.isEmpty() || gradeText.isEmpty()) {
            result.addError("name, date, or grade", "required");
            errorBox.add("Invalid input for name, date, or grade; all are required");
        }

        // 2) Validate date only if nonempty
        int dateVal = 0;
        if (!dateText.isEmpty()) {
            try {
                dateVal = Integer.parseInt(dateText);
            } catch (NumberFormatException e) {
                result.addError("date", "Integer");
                errorBox.add("Invalid input for date; a(n) Integer is required");
            }
        }

        // 3) Validate diameter if provided
        double diameterVal = 0.0;
        if (!diameterText.isEmpty()) {
            try {
                diameterVal = Double.parseDouble(diameterText);
            } catch (NumberFormatException e) {
                result.addError("diameter", "Double");
                errorBox.add("Invalid input for diameter; a(n) Double is required");
            }
        }

        // 4) Validate thickness if provided
        double thicknessVal = 0.0;
        if (!thicknessText.isEmpty()) {
            try {
                thicknessVal = Double.parseDouble(thicknessText);
            } catch (NumberFormatException e) {
                result.addError("thickness", "Double");
                errorBox.add("Invalid input for thickness; a(n) Double is required");
            }
        }

        // 5) Validate weight if provided
        double weightVal = 0.0;
        if (!weightText.isEmpty()) {
            try {
                weightVal = Double.parseDouble(weightText);
            } catch (NumberFormatException e) {
                result.addError("weight", "Double");
                errorBox.add("Invalid input for weight; a(n) Double is required");
            }
        }

        // If any validation errors so far, bail out:
        if (!result.isValid()) {
            return result;
        }

        // 6) All validation passed → construct Coin object and insert into the chosen list:
        Coin coin = new Coin();
        coin.setName(name);
        coin.setDate(dateVal);
        coin.setGrade(gradeText);
        coin.setDiameter(diameterVal);
        coin.setThickness(thicknessVal);
        coin.setEdge(edge);
        coin.setWeight(weightVal);
        coin.setComposition(composition);
        coin.setDenomination(denomination);

        // Insert into exactly the table named `listName`:
        db.insertCoin(listName, coin, null, null);

        result.setCreatedId(coin.getId());
        return result;
    }

    /** Returns all coins in the given list. */
    public List<Coin> listCoins(String listName) {
        return db.getAllCoins(listName);
    }

    /**
     * Search within a given list by attribute/value (partial match for name).
     * Numeric attributes must match exactly.
     *
     * @param listName which table to query
     * @param attr     attribute name (e.g. "name", "date", "diameter", etc.)
     * @param value    text to search for (partial for strings, exact for numerics)
     */
    public List<Coin> searchCoins(String listName, String attr, String value) {
        List<Coin> all = db.getAllCoins(listName);

        return all.stream()
                .filter(c -> {
                    String actual = c.getAttributeValue(attr);
                    // String‐type attributes → partial (case‐insensitive)
                    if ("name".equals(attr)
                            || "grade".equals(attr)
                            || "edge".equals(attr)
                            || "composition".equals(attr)
                            || "denomination".equals(attr)) {
                        return actual.toLowerCase().contains(value.toLowerCase());
                    }
                    // Numeric attributes → exact match
                    try {
                        switch (attr) {
                            case "date":
                                return Integer.parseInt(actual) == Integer.parseInt(value);
                            case "diameter":
                            case "thickness":
                            case "weight":
                                return Double.compare(
                                        Double.parseDouble(actual),
                                        Double.parseDouble(value)
                                ) == 0;
                            default:
                                return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /** Persist edits to an existing Coin in the given list. */
    public boolean saveCoin(String listName, Coin coin) {
        clearErrorBox();
        db.updateCoin(listName, coin);
        return true;
    }

    /** Delete a Coin from the given list. */
    public boolean deleteCoin(String listName, Coin coin) {
        clearErrorBox();
        db.deleteCoin(listName, coin.getId().toString());
        return true;
    }

    /** Look up a Coin by its UUID in the given list. */
    public Coin getCoinById(String listName, String id) {
        return db.getCoinById(listName, id);
    }

    /**
     * Move a Coin from one list to another.
     *
     * @param fromList the source table name
     * @param toList the destination table name
     * @param coin the Coin to move
     * @return true if moved successfully, false otherwise
     */
    public boolean moveCoin(String fromList, String toList, Coin coin) {
        clearErrorBox();
        if (fromList == null || toList == null || coin == null || fromList.equals(toList)) {
            errorBox.add("Invalid move operation");
            return false;
        }
        try {
            // Insert into the destination table
            db.insertCoin(toList, coin, coin.getObverseBytes(), coin.getInverseBytes());
            // Delete from the source table
            db.deleteCoin(fromList, coin.getId().toString());
            return true;
        } catch (Exception e) {
            errorBox.add("Failed to move coin to \"" + toList + "\"");
            return false;
        }
    }

    /** Returns the list of attribute names (in order) for display/search. */
    public List<String> getCoinAttributeNames() {
        return Coin.getAttributeNamesInOrder();
    }


    // ─── FIELD‐ERROR / VALIDATION CLASSES (unchanged) ───────────────────────────────
    public static class FieldError {
        private final String field;
        private final String expectedType;

        public FieldError(String field, String expectedType) {
            this.field = field;
            this.expectedType = expectedType;
        }
        public String getField() { return field; }
        public String getExpectedType() { return expectedType; }
    }

    public static class ValidationResult {
        private final List<FieldError> errors = new ArrayList<>();
        private UUID createdId;

        public List<FieldError> getErrors() { return errors; }
        public boolean isValid() { return errors.isEmpty(); }
        public UUID getCreatedId() { return createdId; }
        public void setCreatedId(UUID id) { this.createdId = id; }
        public void addError(String field, String expectedType) {
            errors.add(new FieldError(field, expectedType));
        }
    }
}