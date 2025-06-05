import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Controller {

    private final Database db;

    public Controller() {
        this.db = new Database();
    }

    /**
     * Wraps validation errors.
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
     * Returned after attempting to create a new coin in a list.
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

    /**
     * Return all saved list names (e.g. “owned”, “wishlist”, plus any custom ones).
     */
    public List<String> getAllListNames() {
        return db.getAllListNames();
    }

    /**
     * Deletes a list with the given name unless it is a protected list ("owned" or "wishlist").
     *
     * @param listName the name of the list to delete
     * @return false if the list is "owned" or "wishlist" (case-insensitive), true if deleted
     */
    public boolean deleteList(String listName) {
        if (listName == null) return false;
        if (listName.equalsIgnoreCase("owned") || listName.equalsIgnoreCase("wishlist")) {
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
     */
    public ValidationResult createCoinInList(String listName, Map<String, String> rawFields) {
        ValidationResult result = new ValidationResult();

        String name        = rawFields.getOrDefault("name", "").trim();
        String dateText    = rawFields.getOrDefault("date", "").trim();
        String gradeText   = rawFields.getOrDefault("grade", "").trim();
        String thicknessText   = rawFields.getOrDefault("thickness", "").trim();
        String diameterText    = rawFields.getOrDefault("diameter", "").trim();
        String composition     = rawFields.getOrDefault("composition", "").trim();
        String denomination    = rawFields.getOrDefault("denomination", "").trim();
        String edge            = rawFields.getOrDefault("edge", "").trim();
        String weightText      = rawFields.getOrDefault("weight", "").trim();

        // 1) Required: name, date, grade
        if (name.isEmpty() || dateText.isEmpty() || gradeText.isEmpty()) {
            result.addError("name, date, or grade", "required");
        }

        int dateVal = 0;
        if (!dateText.isEmpty()) {
            try {
                dateVal = Integer.parseInt(dateText);
            } catch (NumberFormatException e) {
                result.addError("date", "Integer");
            }
        }

        double thicknessVal = 0.0;
        if (!thicknessText.isEmpty()) {
            try {
                thicknessVal = Double.parseDouble(thicknessText);
            } catch (NumberFormatException e) {
                result.addError("thickness", "Double");
            }
        }

        double diameterVal = 0.0;
        if (!diameterText.isEmpty()) {
            try {
                diameterVal = Double.parseDouble(diameterText);
            } catch (NumberFormatException e) {
                result.addError("diameter", "Double");
            }
        }

        double weightVal = 0.0;
        if (!weightText.isEmpty()) {
            try {
                weightVal = Double.parseDouble(weightText);
            } catch (NumberFormatException e) {
                result.addError("weight", "Double");
            }
        }

        if (!result.isValid()) {
            return result;
        }

        // Build Coin object, then persist into the chosen list
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
        db.updateCoin(listName, coin);
        return true;
    }

    /**
     * Delete a Coin from the given list.
     */
    public boolean deleteCoin(String listName, Coin coin) {
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