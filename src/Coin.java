import javafx.scene.image.Image;
import java.util.UUID;

public class Coin {

    private UUID id;  // Removed final so it can be set by Database
    private String name;
    private int date;
    private double thickness;
    private double diameter;
    private Image obverseImage;
    private Image reverseImage;
    private String grade;
    private String composition;
    private String denomination;
    private String edge;
    private double weight;
    private byte[] obverseBytes;
    private byte[] inverseBytes;

    // No-argument constructor: UUID auto-generated, other fields blank/default
    public Coin() {
        this.id = UUID.randomUUID();
        this.name = "";
        this.date = 0;
        this.thickness = 0.0;
        this.diameter = 0.0;
        this.obverseImage = null;
        this.reverseImage = null;
        this.grade = "";
        this.composition = "";
        this.denomination = "";
        this.edge = "";
        this.weight = 0.0;
        this.obverseBytes = null;
        this.inverseBytes = null;
    }

    // Constructor used by Database to set the stored UUID
    public Coin(UUID id) {
        this.id = id;
        this.name = "";
        this.date = 0;
        this.thickness = 0.0;
        this.diameter = 0.0;
        this.obverseImage = null;
        this.reverseImage = null;
        this.grade = "";
        this.composition = "";
        this.denomination = "";
        this.edge = "";
        this.weight = 0.0;
        this.obverseBytes = null;
        this.inverseBytes = null;
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getDate() { return date; }
    public double getThickness() { return thickness; }
    public double getDiameter() { return diameter; }
    public Image getObverseImage() { return obverseImage; }
    public Image getReverseImage() { return reverseImage; }
    public String getGrade() { return grade; }
    public String getComposition() { return composition; }
    public String getDenomination() { return denomination; }
    public String getEdge() { return edge; }
    public double getWeight() { return weight; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDate(int date) { this.date = date; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setDiameter(double diameter) { this.diameter = diameter; }
    public void setObverseImage(Image obverseImage) { this.obverseImage = obverseImage; }
    public void setReverseImage(Image reverseImage) { this.reverseImage = reverseImage; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setComposition(String composition) { this.composition = composition; }
    public void setDenomination(String denomination) { this.denomination = denomination; }
    public void setEdge(String edge) { this.edge = edge; }
    public void setWeight(double weight) { this.weight = weight; }

    public byte[] getObverseBytes() {
        return obverseBytes;
    }

    public void setObverseBytes(byte[] obverseBytes) {
        this.obverseBytes = obverseBytes;
    }

    public byte[] getInverseBytes() {
        return inverseBytes;
    }

    public void setInverseBytes(byte[] inverseBytes) {
        this.inverseBytes = inverseBytes;
    }

    @Override
    public String toString() {
        return "Coin{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", thickness=" + thickness +
                ", diameter=" + diameter +
                ", grade=" + grade +
                ", composition='" + composition + '\'' +
                ", denomination='" + denomination + '\'' +
                ", edge='" + edge + '\'' +
                ", weight=" + weight +
                '}';
    }
}