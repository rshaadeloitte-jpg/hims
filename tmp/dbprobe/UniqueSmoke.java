import java.sql.*;

/** Proves the partial unique index rejects duplicate (unit_id, accession_no) when unit_id is set. */
public class UniqueSmoke {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only")) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("INSERT INTO ihms_equipment.equipment (id, unit_id, item_id, accession_no, working_status) VALUES (900001, 77, 1, 'ACC-TEST', 1)");
                System.out.println("first insert OK");
                try {
                    s.execute("INSERT INTO ihms_equipment.equipment (id, unit_id, item_id, accession_no, working_status) VALUES (900002, 77, 1, 'ACC-TEST', 1)");
                    System.out.println("FAIL duplicate (unit, accession) accepted");
                } catch (SQLException dup) {
                    System.out.println("OK   duplicate (unit_id=77, accession_no='ACC-TEST') rejected -> " + dup.getMessage().split("\n")[0]);
                }
                // different unit must be allowed
                s.execute("INSERT INTO ihms_equipment.equipment (id, unit_id, item_id, accession_no, working_status) VALUES (900003, 78, 1, 'ACC-TEST', 1)");
                System.out.println("OK   same accession under different unit accepted (per-unit uniqueness scope)");
            } finally {
                c.rollback();
                c.setAutoCommit(true);
                System.out.println("smoke-test data rolled back");
            }
        }
    }
}
