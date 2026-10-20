import java.sql.*;

public class DropSchema {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only");
             Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS ihms_equipment CASCADE");
            System.out.println("schema ihms_equipment dropped");
        }
    }
}
