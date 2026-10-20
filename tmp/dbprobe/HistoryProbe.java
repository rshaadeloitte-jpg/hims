import java.sql.*;

public class HistoryProbe {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only");
             Statement s = c.createStatement()) {
            String[] schemas = {"ihms_waste", "ihms_grievance", "ihms_equipment"};
            for (String sc : schemas) {
                System.out.println("=== " + sc + " ===");
                try (ResultSet rs = s.executeQuery(
                        "SELECT installed_rank, version, description, type, script, success FROM " + sc + ".flyway_schema_history ORDER BY installed_rank")) {
                    while (rs.next())
                        System.out.println("  rank=" + rs.getInt(1) + " v=" + rs.getString(2) + " desc=" + rs.getString(3)
                                + " type=" + rs.getString(4) + " script=" + rs.getString(5) + " success=" + rs.getBoolean(6));
                } catch (SQLException e) { System.out.println("  (no history: " + e.getMessage().split("\n")[0] + ")"); }
            }
        }
    }
}
