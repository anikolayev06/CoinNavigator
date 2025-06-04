import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



/**
 * Database class to manage Coin objects using SQLite.
 */
public class Database {

    private static final String DB_URL = "jdbc:sqlite:coins.db";

    public Database() {
        createTableIfNotExists();
    }

    /**
     * Create the "coins" table if it does not already exist.
     */
    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS coins (
                id           TEXT    PRIMARY KEY,
                name         TEXT    NOT NULL,
                date         INTEGER,
                thickness    REAL,
                diameter     REAL,
                grade        TEXT,
                composition  TEXT,
                denomination TEXT,
                edge         TEXT,
                weight       REAL,
                obverse_png  BLOB,
                inverse_png  BLOB
            );
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Ensure obverse_png and inverse_png columns exist if table existed previously
            try {
                stmt.executeUpdate("ALTER TABLE coins ADD COLUMN obverse_png BLOB;");
            } catch (SQLException ignored) {
                // Column already exists: ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE coins ADD COLUMN inverse_png BLOB;");
            } catch (SQLException ignored) {
                // Column already exists: ignore
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert a new Coin into the database.
     * The Coin's UUID is stored as a string.
     *
     * @param coin the Coin object to insert
     */
    public void insertCoin(Coin coin, byte[] obverseBytes, byte[] inverseBytes) {
        String sql = """
            INSERT INTO coins (
                id, name, date, thickness, diameter,
                grade, composition, denomination, edge, weight,
                obverse_png, inverse_png
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, coin.getId().toString());
            pstmt.setString(2, coin.getName());
            pstmt.setInt(3, coin.getDate());
            pstmt.setDouble(4, coin.getThickness());
            pstmt.setDouble(5, coin.getDiameter());
            pstmt.setString(6, coin.getGrade());
            pstmt.setString(7, coin.getComposition());
            pstmt.setString(8, coin.getDenomination());
            pstmt.setString(9, coin.getEdge());
            pstmt.setDouble(10, coin.getWeight());
            pstmt.setBytes(11, obverseBytes);
            pstmt.setBytes(12, inverseBytes);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve a Coin by its UUID.
     *
     * @param id the UUID of the coin as a string
     * @return the Coin object if found, otherwise null
     */
    public Coin getCoinById(String id) {
        String sql = "SELECT * FROM coins WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Coin coin = new Coin(UUID.fromString(rs.getString("id")));
                coin.setName(rs.getString("name"));
                coin.setDate(rs.getInt("date"));
                coin.setThickness(rs.getDouble("thickness"));
                coin.setDiameter(rs.getDouble("diameter"));
                coin.setGrade(rs.getString("grade"));
                coin.setComposition(rs.getString("composition"));
                coin.setDenomination(rs.getString("denomination"));
                coin.setEdge(rs.getString("edge"));
                coin.setWeight(rs.getDouble("weight"));

                byte[] obvBytes = rs.getBytes("obverse_png");
                byte[] invBytes = rs.getBytes("inverse_png");
                coin.setObverseBytes(obvBytes);
                coin.setInverseBytes(invBytes);

                return coin;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieve all coins in the database.
     *
     * @return a list of all Coin objects
     */
    public List<Coin> getAllCoins() {
        List<Coin> coins = new ArrayList<>();
        String sql = "SELECT * FROM coins";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Coin coin = new Coin(UUID.fromString(rs.getString("id")));
                coin.setName(rs.getString("name"));
                coin.setDate(rs.getInt("date"));
                coin.setThickness(rs.getDouble("thickness"));
                coin.setDiameter(rs.getDouble("diameter"));
                coin.setGrade(rs.getString("grade"));
                coin.setComposition(rs.getString("composition"));
                coin.setDenomination(rs.getString("denomination"));
                coin.setEdge(rs.getString("edge"));
                coin.setWeight(rs.getDouble("weight"));

                byte[] obvBytes = rs.getBytes("obverse_png");
                byte[] invBytes = rs.getBytes("inverse_png");
                coin.setObverseBytes(obvBytes);
                coin.setInverseBytes(invBytes);

                coins.add(coin);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coins;
    }

    /**
     * Update an existing coin in the database.
     *
     * @param coin the Coin object with updated fields
     */
    public void updateCoin(Coin coin) {
        String sql = """
            UPDATE coins SET
                name = ?, date = ?, thickness = ?, diameter = ?,
                grade = ?, composition = ?, denomination = ?, edge = ?, weight = ?
            WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, coin.getName());
            pstmt.setInt(2, coin.getDate());
            pstmt.setDouble(3, coin.getThickness());
            pstmt.setDouble(4, coin.getDiameter());
            pstmt.setString(5, coin.getGrade());
            pstmt.setString(6, coin.getComposition());
            pstmt.setString(7, coin.getDenomination());
            pstmt.setString(8, coin.getEdge());
            pstmt.setDouble(9, coin.getWeight());
            pstmt.setString(10, coin.getId().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a coin from the database by its UUID.
     *
     * @param id the UUID of the coin as a string
     */
    public void deleteCoin(String id) {
        String sql = "DELETE FROM coins WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}