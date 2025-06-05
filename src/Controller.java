import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for CoinNavigator.
 *  – Manages errorBox (for GUI to display errors),
 *  – Delegates to Database for CRUD on multiple “lists.”
 */
public class Controller {

    private final Database db;

    /**
     * A simple container of validation errors (field + expected type).
     */
    public static class FieldError {
        private final String field;
        private final String expectedType;

        public FieldError(String field, String expectedType) {
            this.field = field;
            this.expectedType = expectedType;
        }

        public String getField() {
            return field;
        }

        public String getExpectedType() {
            return expectedType;
        }
    }

    /**
     * Holds validation errors (zero or more). If valid, createdId is set.
     */
    public static class ValidationResult {
        private final List<FieldError> errors = new ArrayList<>();
        private UUID createdId;

        public List<FieldError> getErrors() {
            return errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public UUID getCreatedId() {
            return createdId;
        }

        public void setCreatedId(UUID id) {
            this.createdId = id;
        }

        public void addError(String field, String expectedType) {
            errors.add(new FieldError(field, expectedType));
        }
    }

    // ——— “Error Box” storage —————————————————————————————————————

    /**
     * This List holds plain‐text error messages (one or more).
     * Whenever a controller method fails (e.g. validation error or trying to delete a protected list),
     * we push one or more strings here. GUI will render them in red at the top.
     */
    private final List<String> errorBox = new ArrayList<>();

    /**
     * Return all current error messages (as plain text). GUI reads this and displays them.
     */
    public List<String> getErrorBox() {
        return errorBox;
    }

    /**
     * Clear any stored error messages. GUI should call this before invoking an action.
     */
    public void clearErrorBox() {
        errorBox.clear();
    }

    // ——— end “Error Box” storage ————————————————————————————————————

    public Controller() {
        this.db = new Database();
    }

    /**
     * Return all saved list names (e.g. “Owned”, “Wishlist”, plus any custom ones).
     */
    public List<String> getAllListNames() {
        return db.getAllListNames();
    }

    /**
     * Deletes a list with the given name unless it is a protected list ("Owned" or "Wishlist").
     *
     * @param listName the name of the list to delete
     * @return false if the list is "Owned" or "Wishlist" (case-insensitive), true if deleted
     */
    public boolean deleteList(String listName) {
        if (listName == null) {
            return false;
        }
        if (listName.equalsIgnoreCase("Owned") || listName.equalsIgnoreCase("Wishlist")) {
            // Protected: push error into errorBox
            errorBox.add("Cannot delete Owned or Wishlist");
            return false;
        }
        db.deleteList(listName);
        return true;
    }

    /**
     * Called when the user clicks “+” to make a brand‐new list. Persists it to metadata.
     */
    public void createList(String listName) {
        db.createList(listName);
    }

    /**
     * Creates a new Coin in the specified list/table (validates inputs).
     * If validation fails, errorBox is populated with one or more messages.
     *
     * @param listName  the list/table to insert into
     * @param rawFields a map with keys: "name", "date", "grade", "thickness", "diameter",
     *                  "composition", "denomination", "edge", "weight"
     * @return ValidationResult, which contains any FieldError(s). If valid, createdId is set.
     */
    public ValidationResult createCoinInList(String listName, Map<String, String> rawFields) {
        // Always clear the errorBox before validating
        clearErrorBox();

        ValidationResult result = new ValidationResult();

        String name            = rawFields.getOrDefault("name", "").trim();
        String dateText        = rawFields.getOrDefault("date", "").trim();
        String gradeText       = rawFields.getOrDefault("grade", "").trim();
        String thicknessText   = rawFields.getOrDefault("thickness", "").trim();
        String diameterText    = rawFields.getOrDefault("diameter", "").trim();
        String composition     = rawFields.getOrDefault("composition", "").trim();
        String denomination    = rawFields.getOrDefault("denomination", "").trim();
        String edge            = rawFields.getOrDefault("edge", "").trim();
        String weightText      = rawFields.getOrDefault("weight", "").trim();

        // 1) Required: name, date, grade
        if (name.isEmpty() || dateText.isEmpty() || gradeText.isEmpty()) {
            result.addError("name, date, or grade", "required");
            errorBox.add("Invalid input for name, date, or grade; all are required");
        }

        // 2) Validate date only if provided
        int dateVal = 0;
        if (!dateText.isEmpty()) {
            try {
                dateVal = Integer.parseInt(dateText);
            } catch (NumberFormatException e) {
                result.addError("date", "Integer");
                errorBox.add("Invalid input for date; a(n) Integer is required");
            }
        }

        // 3) Validate thickness if provided
        double thicknessVal = 0.0;
        if (!thicknessText.isEmpty()) {
            try {
                thicknessVal = Double.parseDouble(thicknessText);
            } catch (NumberFormatException e) {
                result.addError("thickness", "Double");
                errorBox.add("Invalid input for thickness; a(n) Double is required");
            }
        }

        // 4) Validate diameter if provided
        double diameterVal = 0.0;
        if (!diameterText.isEmpty()) {
            try {
                diameterVal = Double.parseDouble(diameterText);
            } catch (NumberFormatException e) {
                result.addError("diameter", "Double");
                errorBox.add("Invalid input for diameter; a(n) Double is required");
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

        // If any validation errors, return immediately
        if (!result.isValid()) {
            return result;
        }

        // 6) All validation passed → build Coin object, then insert into the chosen list
        Coin coin = new Coin();
        coin.setName(name);
        coin.setDate(dateVal);
        coin.setGrade(gradeText);
        coin.setThickness(thicknessVal);
        coin.setDiameter(diameterVal);
        coin.setComposition(composition);
        coin.setDenomination(denomination);
        coin.setEdge(edge);
        coin.setWeight(weightVal);

        db.insertCoin(listName, coin, null, null);
        result.setCreatedId(coin.getId());
        return result;
    }

    /**
     * Returns all coins in the given list.
     */
    public List<Coin> listCoins(String listName) {
        return db.getAllCoins(listName);
    }

    /**
     * Search within a given list by attribute/value (partial match for name).
     */
    public List<Coin> searchCoins(String listName, String attr, String value) {
        List<Coin> all = db.getAllCoins(listName);
        return all.stream()
                .filter(c -> attributeMatches(c, attr, value))
                .collect(Collectors.toList());
    }

    /**
     * Persist edits to an existing Coin in the given list.
     */
    public boolean saveCoin(String listName, Coin coin) {
        clearErrorBox();
        db.updateCoin(listName, coin);
        return true;
    }

    /**
     * Delete a Coin from the given list.
     */
    public boolean deleteCoin(String listName, Coin coin) {
        clearErrorBox();
        db.deleteCoin(listName, coin.getId().toString());
        return true;
    }

    /**
     * Look up a Coin by its UUID in the given list.
     */
    public Coin getCoinById(String listName, String id) {
        return db.getCoinById(listName, id);
    }

    /**
     * Returns the list of searchable attributes (used by GUI).
     */
    public List<String> getSearchableAttributes() {
        return List.of(
                "id", "name", "date", "thickness", "diameter",
                "grade", "composition", "denomination", "edge", "weight"
        );
    }

    /**
     * Helper: checks a Coin’s field against the given value.
     */
    private boolean attributeMatches(Coin c, String attr, String value) {
        try {
            switch (attr) {
                case "id":
                    return c.getId().toString().equalsIgnoreCase(value);
                case "name":
                    return c.getName().toLowerCase().contains(value.toLowerCase());
                case "date":
                    return c.getDate() == Integer.parseInt(value);
                case "thickness":
                    return Double.compare(c.getThickness(), Double.parseDouble(value)) == 0;
                case "diameter":
                    return Double.compare(c.getDiameter(), Double.parseDouble(value)) == 0;
                case "grade":
                    return c.getGrade().equalsIgnoreCase(value);
                case "composition":
                    return c.getComposition().equalsIgnoreCase(value);
                case "denomination":
                    return c.getDenomination().equalsIgnoreCase(value);
                case "edge":
                    return c.getEdge().equalsIgnoreCase(value);
                case "weight":
                    return Double.compare(c.getWeight(), Double.parseDouble(value)) == 0;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}