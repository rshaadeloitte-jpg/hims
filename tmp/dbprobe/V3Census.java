import java.sql.*;
public class V3Census {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only")) {
            q(c, "flyway history", "SELECT version || '=' || success FROM ihms_equipment.flyway_schema_history ORDER BY installed_rank");
            q(c, "tables(27=24+3)", "SELECT count(*) FROM information_schema.tables WHERE table_schema='ihms_equipment'");
            q(c, "sequences(27=24+3)", "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='ihms_equipment'");
            q(c, "pm cols sample", "SELECT column_name || ':' || data_type FROM information_schema.columns WHERE table_schema='ihms_equipment' AND table_name='eqp_preventive_maintenance' AND column_name IN ('functional_check_remark','amount_incurred','pm_status','remarks')");
        }
    }
    static void q(Connection c, String label, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            StringBuilder sb = new StringBuilder();
            while (rs.next()) sb.append(rs.getString(1)).append(" | ");
            System.out.println(label + " => " + sb);
        }
    }
}
