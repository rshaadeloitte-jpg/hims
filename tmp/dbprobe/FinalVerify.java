import java.sql.*;
import java.util.*;

/** Final verification of the Flyway-built ihms_equipment schema. */
public class FinalVerify {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only");
             Statement s = c.createStatement()) {

            // history parity with sibling services
            System.out.println("=== flyway_schema_history ===");
            try (ResultSet rs = s.executeQuery(
                    "SELECT installed_rank, version, description, type, success FROM ihms_equipment.flyway_schema_history ORDER BY installed_rank")) {
                while (rs.next())
                    System.out.println("  rank=" + rs.getInt(1) + " v=" + rs.getString(2) + " desc=" + rs.getString(3)
                            + " type=" + rs.getString(4) + " success=" + rs.getBoolean(5));
            }

            check(s, "tables (19 + history)", "SELECT count(*) FROM information_schema.tables WHERE table_schema='ihms_equipment'", 20L);
            check(s, "sequences", "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='ihms_equipment'", 19L);
            check(s, "complaint_status_master rows", "SELECT count(*) FROM ihms_equipment.complaint_status_master", 4L);
            check(s, "working_status_master rows", "SELECT count(*) FROM ihms_equipment.equipment_working_status_master", 4L);
            check(s, "maintenance_type_master rows", "SELECT count(*) FROM ihms_equipment.maintenance_type_master", 3L);

            List<String> audit = List.of("unit_id","status","created_unit_id","updated_unit_id",
                    "added_by","added_datetime","updated_by","updated_datetime","synchronized");
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='ihms_equipment' AND table_name <> 'flyway_schema_history' ORDER BY 1")) {
                while (rs.next()) tables.add(rs.getString(1));
            }
            int bad = 0;
            for (String t : tables) {
                Set<String> cols = new HashSet<>();
                try (ResultSet rs = s.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_schema='ihms_equipment' AND table_name='" + t + "'")) {
                    while (rs.next()) cols.add(rs.getString(1));
                }
                for (String a : audit) if (!cols.contains(a)) { System.out.println("MISSING " + a + " on " + t); bad++; }
            }
            System.out.println(bad == 0 ? "audit columns OK on all " + tables.size() + " business tables" : bad + " GAPS");

            try (ResultSet rs = s.executeQuery("SELECT nextval('ihms_equipment.equipment_seq')")) {
                rs.next();
                System.out.println("equipment_seq nextval -> " + rs.getLong(1));
            }

            // FK + partial-unique-index smoke test (rolled back)
            c.setAutoCommit(false);
            try {
                s.execute("INSERT INTO ihms_equipment.equipment (id, item_id, accession_no, working_status) VALUES (900001, 1, 'ACC-TEST', 1)");
                try {
                    s.execute("INSERT INTO ihms_equipment.equipment (id, item_id, accession_no, working_status) VALUES (900002, 1, 'ACC-TEST', 1)");
                    System.out.println("FAIL duplicate accession in same unit was accepted");
                } catch (SQLException dup) {
                    System.out.println("OK   duplicate (unit, accession) rejected: " + dup.getMessage().split("\n")[0]);
                }
                s.execute("INSERT INTO ihms_equipment.equipment_detail_docs (id, equipment_id, doc_type) VALUES (900101, 900001, 'MANUAL')");
                System.out.println("OK   equipment_detail_docs FK to equipment enforced path works");
            } finally {
                c.rollback();
                c.setAutoCommit(true);
                System.out.println("smoke-test data rolled back");
            }
        }
    }

    static void check(Statement s, String label, String q, long expected) throws SQLException {
        try (ResultSet rs = s.executeQuery(q)) {
            rs.next();
            long got = rs.getLong(1);
            System.out.println((got == expected ? "OK   " : "FAIL ") + label + ": " + got + (got == expected ? "" : " (expected " + expected + ")"));
        }
    }
}
