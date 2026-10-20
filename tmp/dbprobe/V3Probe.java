import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * V3__preventive_maintenance_tables.sql verifier (TAS-9).
 * Runs the whole script TWICE inside one transaction against the shared PG
 * (pass 1 = apply, pass 2 = idempotency proof), smoke-tests constraints,
 * then ROLLBACK — leaves the shared DB untouched so real Flyway can record
 * V3 later in the production-cutover shape.
 */
public class V3Probe {

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        String sql = Files.readString(Path.of(
                "services/equipment-service/src/main/resources/db/migration/V3__preventive_maintenance_tables.sql"));
        List<String> stmts = split(sql);
        System.out.println("parsed statements: " + stmts.size());

        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only")) {
            c.setAutoCommit(false);
            try {
                for (int pass = 1; pass <= 2; pass++) {
                    int n = 0;
                    for (String s : stmts) {
                        try (Statement st = c.createStatement()) { st.execute(s); }
                        n++;
                    }
                    System.out.println("PASS " + pass + ": executed " + n + " statements OK"
                            + (pass == 2 ? "  (idempotent)" : ""));
                }

                // ---- object census -------------------------------------------------
                expect(c, "3 PM tables present",
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema='ihms_equipment' "
                                + "AND table_name IN ('eqp_pm_schedule','eqp_pm_schedule_details','eqp_preventive_maintenance')",
                        3L);
                expect(c, "3 PM sequences present",
                        "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='ihms_equipment' "
                                + "AND sequence_name IN ('eqp_pm_schedule_seq','eqp_pm_schedule_details_seq','eqp_preventive_maintenance_seq')",
                        3L);
                expect(c, "6 indexes present (uq + 5 ix)",
                        "SELECT count(*) FROM pg_indexes WHERE schemaname='ihms_equipment' AND indexname IN "
                                + "('uq_eqp_pm_schedule_code','ix_eqp_pm_schedule_equipment','ix_eqp_pm_schedule_due',"
                                + "'ix_eqp_pm_schedule_unit','ix_eqp_pm_schedule_details_schedule','ix_eqp_pm_schedule_details_date',"
                                + "'ix_eqp_preventive_maintenance_schedule','ix_eqp_preventive_maintenance_equipment','ix_eqp_preventive_maintenance_date')",
                        9L);

                // ---- happy-path rows (rolled back) ---------------------------------
                exec(c, "INSERT INTO ihms_equipment.equipment (id, item_id) VALUES (990001, 501)");
                exec(c, "INSERT INTO ihms_equipment.eqp_pm_schedule (id, equipment_id, schedule_code, frequency, jan, mar, next_due_date, pm_type) "
                        + "VALUES (990002, 990001, 'PM-SCH-PROBE', 6, TRUE, TRUE, '2026-09-01', 1)");
                exec(c, "INSERT INTO ihms_equipment.eqp_pm_schedule_details (id, pm_schedule_id, scheduled_date) VALUES (990003, 990002, '2026-09-01')");
                exec(c, "INSERT INTO ihms_equipment.eqp_preventive_maintenance (id, equipment_id, pm_schedule_id, maintenance_no, maintenance_date, "
                        + "performed_by, functional_check_remark, remarks, amount_incurred, service_agent_id) "
                        + "VALUES (990004, 990001, 990002, 'PM-2026-PROBE', '2026-09-01', 42, 'All checks OK', 'generic remarks col', 1234.56, 7)");

                long seqSum = single(c, "SELECT nextval('ihms_equipment.eqp_pm_schedule_seq')::bigint "
                        + "+ nextval('ihms_equipment.eqp_pm_schedule_details_seq')::bigint "
                        + "+ nextval('ihms_equipment.eqp_preventive_maintenance_seq')::bigint");
                check("nextval works on all 3 sequences (sum=" + seqSum + ")", seqSum == 3L);

                // ---- guard paths ----------------------------------------------------
                violates(c, "duplicate schedule_code rejected by uq partial index", "uq_eqp_pm_schedule_code",
                        "INSERT INTO ihms_equipment.eqp_pm_schedule (id, equipment_id, schedule_code) VALUES (990005, 990001, 'PM-SCH-PROBE')");
                violates(c, "pm_type=9 rejected by CHECK", "eqp_pm_schedule_pm_type_check",
                        "INSERT INTO ihms_equipment.eqp_pm_schedule (id, equipment_id, pm_type) VALUES (990006, 990001, 9)");
                violates(c, "detail line with unknown schedule rejected by FK", "eqp_pm_schedule_details_pm_schedule_id_fkey",
                        "INSERT INTO ihms_equipment.eqp_pm_schedule_details (id, pm_schedule_id) VALUES (990007, 987654)");
                violates(c, "completion with unknown schedule rejected by FK", "eqp_preventive_maintenance_pm_schedule_id_fkey",
                        "INSERT INTO ihms_equipment.eqp_preventive_maintenance (id, equipment_id, pm_schedule_id) VALUES (990008, 990001, 987654)");
                violates(c, "completion with unknown equipment rejected by FK", "eqp_preventive_maintenance_equipment_id_fkey",
                        "INSERT INTO ihms_equipment.eqp_preventive_maintenance (id, equipment_id) VALUES (990009, 987654)");
                violates(c, "pm_status=9 rejected by CHECK", "eqp_preventive_maintenance_pm_status_check",
                        "INSERT INTO ihms_equipment.eqp_preventive_maintenance (id, equipment_id, pm_status) VALUES (990010, 990001, 9)");
            } finally {
                c.rollback();
            }
            try (Statement st = c.createStatement()) {
                ResultSet rs = st.executeQuery(
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema='ihms_equipment' AND table_name LIKE 'eqp_pm%'");
                rs.next();
                check("ROLLBACK left shared DB untouched (0 eqp_pm% tables)", rs.getLong(1) == 0L);
            }
        }
        System.out.println(failures == 0 ? "ALL V3 PROBE CHECKS PASSED" : failures + " CHECKS FAILED");
        if (failures > 0) System.exit(1);
    }

    static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) { st.execute(sql); }
    }

    static long single(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getLong(1); }
    }

    static void expect(Connection c, String what, String sql, long want) throws SQLException {
        long got = single(c, sql);
        check(what + " -> want " + want + " got " + got, got == want);
    }

    static void violates(Connection c, String what, String fragment, String sql) {
        try { exec(c, "SAVEPOINT probe_sp"); } catch (SQLException ignore) { }
        try { exec(c, sql); check(what + " -> NOT violated (BAD)", false); }
        catch (SQLException e) {
            boolean ok = e.getMessage() != null && e.getMessage().contains(fragment)
                    || e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().contains(fragment);
            check(what + " -> " + firstLine(e), ok);
        }
        try { exec(c, "ROLLBACK TO SAVEPOINT probe_sp"); } catch (SQLException ignore) { }
    }

    static String firstLine(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        return "\"" + m.split("\n")[0] + "\"";
    }

    static synchronized void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) failures++;
    }

    /** Split on top-level ';'. Three-state scanner: -- line comments are skipped
     *  whole (so apostrophes inside prose can't corrupt quote tracking) while
     *  single-quoted literals keep their semicolons literal. */
    static List<String> split(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        final int NORMAL = 0, IN_STR = 1, IN_COMMENT = 2;
        int state = NORMAL;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            switch (state) {
                case IN_STR -> {
                    cur.append(ch);
                    if (ch == '\'') state = NORMAL;
                }
                case IN_COMMENT -> {
                    if (ch == '\n') { state = NORMAL; cur.append('\n'); }
                }
                default -> {
                    if (ch == '\'' ) { state = IN_STR; cur.append(ch); }
                    else if (ch == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') state = IN_COMMENT;
                    else if (ch == ';') {
                        String s = cur.toString().trim();
                        if (!s.isEmpty()) out.add(s);
                        cur.setLength(0);
                    } else cur.append(ch);
                }
            }
        }
        String tail = cur.toString().trim();
        if (!tail.isEmpty()) out.add(tail);
        return out;
    }
}
