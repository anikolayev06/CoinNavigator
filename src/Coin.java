import javafx.scene.image.Image;
import java.util.*;
import java.lang.reflect.Field;

/**
 * A simple JavaBean for a single coin.  “id” is auto‐generated or set by the Database.
 *
 * Two utility methods (getAttributeNamesInOrder, getAttributeValue/setAttributeValue)
 * allow the Controller/GUI to loop over “all coin fields” without writing one getter call per field.
 */
public class Coin {

    // ─── FIELDS ────────────────────────────────────────────────────────────────────
    private UUID id;                      // always present but never shown/edited in the GUI
    private String name;
    private int date;
    private String grade;
    private double diameter;
    private double thickness;
    private String edge;
    private double weight;
    private String composition;
    private String denomination;

    // We keep these around (for future image support) but GUI does not show/edit them
    private Image obverseImage;
    private Image reverseImage;
    private byte[] obverseBytes;
    private byte[] inverseBytes;

    // ─── CONSTRUCTORS ───────────────────────────────────────────────────────────────
    /** No-arg constructor: auto-generates a new UUID; everything else blank/zero. */
    public Coin() {
        this.id = UUID.randomUUID();
        this.name = "";
        this.date = 0;
        this.grade = "";
        this.diameter = 0.0;
        this.thickness = 0.0;
        this.edge = "";
        this.weight = 0.0;
        this.composition = "";
        this.denomination = "";
        this.obverseImage = null;
        this.reverseImage = null;
        this.obverseBytes = null;
        this.inverseBytes = null;
    }

    /** Constructor used internally by Database when loading from SQL: sets the stored UUID. */
    public Coin(UUID id) {
        this();     // initialize everything else to defaults
        this.id = id;
    }

    // ─── GETTERS & SETTERS ─────────────────────────────────────────────────────────
    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getDate() { return date; }
    public String getGrade() { return grade; }
    public double getDiameter() { return diameter; }
    public double getThickness() { return thickness; }
    public String getEdge() { return edge; }
    public double getWeight() { return weight; }
    public String getComposition() { return composition; }
    public String getDenomination() { return denomination; }

    public Image getObverseImage() { return obverseImage; }
    public Image getReverseImage() { return reverseImage; }
    public byte[] getObverseBytes() { return obverseBytes; }
    public byte[] getInverseBytes() { return inverseBytes; }

    public void setName(String name) { this.name = name; }
    public void setDate(int date) { this.date = date; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setDiameter(double diameter) { this.diameter = diameter; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setEdge(String edge) { this.edge = edge; }
    public void setWeight(double weight) { this.weight = weight; }
    public void setComposition(String composition) { this.composition = composition; }
    public void setDenomination(String denomination) { this.denomination = denomination; }

    public void setObverseImage(Image obverseImage) { this.obverseImage = obverseImage; }
    public void setReverseImage(Image reverseImage) { this.reverseImage = reverseImage; }
    public void setObverseBytes(byte[] obverseBytes) { this.obverseBytes = obverseBytes; }
    public void setInverseBytes(byte[] inverseBytes) { this.inverseBytes = inverseBytes; }

    // ─── ATTRIBUTE LIST & GENERIC ACCESS ────────────────────────────────────────────

    /**
     * Returns a List of the coin’s “data fields” (in declared order).
     * Any code that wants “all coin fields to show or edit” should call this
     * instead of hard-coding the names.  (Note: we exclude “id” and any images/bytes here.)
     */
    public static List<String> getAttributeNamesInOrder() {
        List<String> attrs = new ArrayList<>();
        for (Field f : Coin.class.getDeclaredFields()) {
            String name = f.getName();
            Class<?> t = f.getType();
            // Skip id, images, byte arrays, and UUID
            if (name.equals("id") || t.equals(Image.class) || t.equals(byte[].class) || t.equals(UUID.class)) {
                continue;
            }
            // Only include primitive and String attributes
            if (t.equals(String.class) || t.equals(int.class) || t.equals(double.class)) {
                attrs.add(name);
            }
        }
        return attrs;
    }

    /**
     * Given one of the attribute names from getAttributeNamesInOrder(),
     * returns its value (as a String) from this Coin.
     *
     * If the field is numeric, we convert to String.  Never returns null.
     */
    public String getAttributeValue(String attr) {
        try {
            Field f = Coin.class.getDeclaredField(attr);
            f.setAccessible(true);
            Object val = f.get(this);
            if (val == null) {
                return "";
            }
            return val.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return "";
        }
    }

    /**
     * Sets the given attribute (by name) to the provided textual value,
     * parsing as needed for integers/doubles.  If parsing fails, throws NumberFormatException.
     */
    public void setAttributeValue(String attr, String textValue) {
        try {
            Field f = Coin.class.getDeclaredField(attr);
            f.setAccessible(true);
            if (f.getType().equals(String.class)) {
                f.set(this, textValue);
            } else if (f.getType().equals(int.class)) {
                f.setInt(this, Integer.parseInt(textValue));
            } else if (f.getType().equals(double.class)) {
                f.setDouble(this, Double.parseDouble(textValue));
            }
        } catch (NoSuchFieldException | IllegalAccessException | NumberFormatException e) {
            // do nothing
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Coin{");
        sb.append("id=").append(id);
        for (String attr : getAttributeNamesInOrder()) {
            sb.append(", ").append(attr).append("=").append(getAttributeValue(attr));
        }
        sb.append("}");
        return sb.toString();
    }
}