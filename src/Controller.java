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
     * A simple data holder for validation errors, pairing a field name
     * with the type that was expected.
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
     * The result returned by createCoin(...), which holds any validation errors
     * and—if validation passed—the UUID of the newly created Coin.
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
     * Creates a new Coin from raw string inputs. Performs the same checks
     * that previously lived in the GUI (required fields, numeric parsing, etc.).
     *
     * @param rawFields a map with keys:
     *                  "name", "date", "grade", "thickness", "diameter",
     *                  "composition", "denomination", "edge", "weight"
     * @return ValidationResult, which contains any FieldError(s). If valid, getCreatedId() is set.
     */
    public ValidationResult createCoin(Map<String, String> rawFields) {
        ValidationResult result = new ValidationResult();

        // Pull each raw string (may be empty)
        String name        = rawFields.getOrDefault("name", "").trim();
        String dateText    = rawFields.getOrDefault("date", "").trim();
        String gradeText   = rawFields.getOrDefault("grade", "").trim();
        String thicknessText   = rawFields.getOrDefault("thickness", "").trim();
        String diameterText    = rawFields.getOrDefault("diameter", "").trim();
        String composition     = rawFields.getOrDefault("composition", "").trim();
        String denomination    = rawFields.getOrDefault("denomination", "").trim();
        String edge            = rawFields.getOrDefault("edge", "").trim();
        String weightText      = rawFields.getOrDefault("weight", "").trim();

        // 1) Required: name, date, grade (if any one is empty, we flag a combined error)
        if (name.isEmpty() || dateText.isEmpty() || gradeText.isEmpty()) {
            result.addError("name, date, or grade", "required");
        }

        // 2) Validate date only if non-empty
        int dateVal = 0;
        if (!dateText.isEmpty()) {
            try {
                dateVal = Integer.parseInt(dateText);
            } catch (NumberFormatException e) {
                result.addError("date", "Integer");
            }
        }

        // 3) Validate thickness if provided
        double thicknessVal = 0.0;
        if (!thicknessText.isEmpty()) {
            try {
                thicknessVal = Double.parseDouble(thicknessText);
            } catch (NumberFormatException e) {
                result.addError("thickness", "Double");
            }
        }

        // 4) Validate diameter if provided
        double diameterVal = 0.0;
        if (!diameterText.isEmpty()) {
            try {
                diameterVal = Double.parseDouble(diameterText);
            } catch (NumberFormatException e) {
                result.addError("diameter", "Double");
            }
        }

        // 5) Validate weight if provided
        double weightVal = 0.0;
        if (!weightText.isEmpty()) {
            try {
                weightVal = Double.parseDouble(weightText);
            } catch (NumberFormatException e) {
                result.addError("weight", "Double");
            }
        }

        // Composition, denomination, and edge: treated as free-form strings. No parsing errors.

        // If there were any errors, return them now:
        if (!result.isValid()) {
            return result;
        }

        // 6) All validation passed → construct and persist the new Coin
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

        // Note: Database.insertCoin(...) signature was changed to accept (Coin, obverseBytes, inverseBytes)
        // In this context we pass null for the image bytes.
        db.insertCoin(coin, null, null);

        result.setCreatedId(coin.getId());
        return result;
    }

    /**
     * Retrieve all coins from the database.
     */
    public List<Coin> listCoins() {
        return db.getAllCoins();
    }

    /**
     * Search coins by attribute → returns matching list.
     */
    public List<Coin> searchCoins(String attr, String value) {
        List<Coin> all = db.getAllCoins();
        return all.stream()
                .filter(c -> attributeMatches(c, attr, value))
                .collect(Collectors.toList());
    }

    /**
     * Persist any edits made to an existing coin.
     */
    public boolean saveCoin(Coin coin) {
        db.updateCoin(coin);
        return true;
    }

    /**
     * Remove a coin from the database.
     */
    public boolean deleteCoin(Coin coin) {
        db.deleteCoin(coin.getId().toString());
        return true;
    }

    /**
     * Look up a coin by its UUID string.
     */
    public Coin getCoinById(String id) {
        return db.getCoinById(id);
    }

    /**
     * List of valid searchable attributes (used by the GUI’s ComboBox).
     */
    public List<String> getSearchableAttributes() {
        return List.of(
                "id", "name", "date", "thickness", "diameter",
                "grade", "composition", "denomination", "edge", "weight"
        );
    }

    /**
     * Utility that matches a single coin’s field against the given value.
     * Used by searchCoins(...) above.
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