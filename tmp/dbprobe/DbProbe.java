import java.sql.*;

public class DbProbe {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/ihms";
        try (Connection c = DriverManager.getConnection(url, "ihms", "ihms-dev-only")) {
            DatabaseMetaData md = c.getMetaData();
            System.out.println("Connected. Product=" + md.getDatabaseProductVersion());
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT nspname FROM pg_namespace WHERE nspname NOT LIKE 'pg_%' AND nspname <> 'information_schema' ORDER BY 1")) {
                System.out.print("Schemas: ");
                StringBuilder sb = new StringBuilder();
                while (rs.next()) sb.append(rs.getString(1)).append(" ");
                System.out.println(sb.toString().trim());
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT schemaname, tablename FROM pg_tables WHERE schemaname IN ('ihms_equipment','ihms_waste','ihms_grievance') ORDER BY 1,2")) {
                while (rs.next()) System.out.println("table: " + rs.getString(1) + "." + rs.getString(2));
            }
        }
    }
}
