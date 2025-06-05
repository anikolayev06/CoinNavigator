import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Database class to manage Coin objects using SQLite.
 * Now supports multiple “lists” (each list = its own table).
 */
public class Database {

    private static final String DB_URL = "jdbc:sqlite:coins.db";

    public Database() {
        // 1) Create the metadata table “lists” if it doesn’t exist yet.
        createListsMetadata();

        // 2) Ensure default lists exist:
        createList("owned");
        createList("wishlist");
    }

    /**
     * Creates the metadata table “lists” if it doesn’t exist.
     * This table simply tracks every named list (i.e. every separate coin‐table).
     */
    private void createListsMetadata() {
        String sql = """
            CREATE TABLE IF NOT EXISTS lists (
                name TEXT PRIMARY KEY
            );
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a brand‐new list (table) named listName, if it doesn't already exist,
     * then records that listName into the metadata table “lists”.
     *
     * @param listName the new list’s name (e.g. “myCustomList”)
     */
    public void createList(String listName) {
        // 1) Create the actual coin‐table for listName (same schema as before).
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS "%s" (
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
            """, listName);

        // 2) Insert listName into the “lists” metadata (if not already present).
        String insertListSql = "INSERT OR IGNORE INTO lists(name) VALUES(?)";

        // Create the coin‐table itself if not exists:
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Record the new listName into the metadata table:
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertListSql)) {
            pstmt.setString(1, listName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns all list names (i.e. all entries in “lists”).
     */
    public List<String> getAllListNames() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT name FROM lists";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Insert a new Coin into the specified list/table.
     *
     * @param listName      the table to insert into (e.g. "owned" or "wishlist" or any custom name)
     * @param coin          the Coin object
     * @param obverseBytes  raw PNG bytes for obverse (may be null)
     * @param inverseBytes  raw PNG bytes for inverse (may be null)
     */
    public void insertCoin(String listName, Coin coin, byte[] obverseBytes, byte[] inverseBytes) {
        String sql = String.format("""
            INSERT INTO "%s" (
                id, name, date, thickness, diameter,
                grade, composition, denomination, edge, weight,
                obverse_png, inverse_png
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, listName);

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
     * Retrieve a Coin by its UUID from a specified list/table.
     *
     * @param listName the table to query
     * @param id       the UUID of the coin as a string
     * @return the Coin object if found, otherwise null
     */
    public Coin getCoinById(String listName, String id) {
        String sql = String.format("SELECT * FROM \"%s\" WHERE id = ?", listName);
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
                coin.setObverseBytes(rs.getBytes("obverse_png"));
                coin.setInverseBytes(rs.getBytes("inverse_png"));
                return coin;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieve all coins in a given list/table.
     *
     * @param listName the table to query
     * @return a list of all Coin objects from that table
     */
    public List<Coin> getAllCoins(String listName) {
        List<Coin> coins = new ArrayList<>();
        String sql = String.format("SELECT * FROM \"%s\"", listName);

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
                coin.setObverseBytes(rs.getBytes("obverse_png"));
                coin.setInverseBytes(rs.getBytes("inverse_png"));
                coins.add(coin);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coins;
    }

    /**
     * Update an existing coin in a given list/table.
     *
     * @param listName the table to update
     * @param coin     the Coin object with updated fields
     */
    public void updateCoin(String listName, Coin coin) {
        String sql = String.format("""
            UPDATE "%s" SET
                name = ?, date = ?, thickness = ?, diameter = ?,
                grade = ?, composition = ?, denomination = ?, edge = ?, weight = ?
            WHERE id = ?
            """, listName);

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
     * Delete a coin by its UUID from a specified list/table.
     *
     * @param listName the table to delete from
     * @param id       the UUID of the coin as a string
     */
    public void deleteCoin(String listName, String id) {
        String sql = String.format("DELETE FROM \"%s\" WHERE id = ?", listName);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * (Optional) If you ever need to delete an entire list/table:
     * 1) drop the table itself,
     * 2) remove it from the “lists” metadata.
     */
    public void deleteList(String listName) {
        // 1) Drop the table:
        String dropSql = String.format("DROP TABLE IF EXISTS \"%s\"", listName);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2) Remove from metadata:
        String removeMetaSql = "DELETE FROM lists WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(removeMetaSql)) {
            pstmt.setString(1, listName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}