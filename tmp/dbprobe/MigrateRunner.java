import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Executes a SQL script against PostgreSQL with a quote-aware statement
 * splitter (-- comments stripped, ';' inside '...' literals respected),
 * then RE-RUNS the whole script to prove idempotency, then verifies the
 * resulting schema state. Used to verify V1__init_ihms_equipment.sql.
 */
public class MigrateRunner {
    public static void main(String[] args) throws Exception {
        String script = Files.readString(Path.of(args[0]));
        String url = "jdbc:postgresql://localhost:5432/ihms";
        List<String> stmts = split(script);
        System.out.println("Parsed " + stmts.size() + " statements");
        try (Connection c = DriverManager.getConnection(url, "ihms", "ihms-dev-only")) {
            c.setAutoCommit(true);
            runOnce(c, stmts, "PASS 1 (initial apply)");
            runOnce(c, stmts, "PASS 2 (idempotency re-run)");
            verify(c);
        }
    }

    static void runOnce(Connection c, List<String> stmts, String label) throws SQLException {
        System.out.println("--- " + label + " ---");
        int n = 0;
        try (Statement s = c.createStatement()) {
            for (String st : stmts) {
                if (st.isBlank()) continue;
                s.execute(st);
                n++;
            }
        }
        System.out.println("Executed " + n + " statements OK");
    }

    static void verify(Connection c) throws SQLException {
        System.out.println("--- verification ---");
        check(c, "tables", "SELECT count(*) FROM information_schema.tables WHERE table_schema='ihms_equipment'", 19L);
        check(c, "sequences", "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='ihms_equipment'", 19L);
        check(c, "complaint_status_master rows", "SELECT count(*) FROM ihms_equipment.complaint_status_master", 4L);
        check(c, "working_status_master rows", "SELECT count(*) FROM ihms_equipment.equipment_working_status_master", 4L);
        check(c, "maintenance_type_master rows", "SELECT count(*) FROM ihms_equipment.maintenance_type_master", 3L);
        // every table must carry the full base audit column set
        List<String> audit = List.of("unit_id","status","created_unit_id","updated_unit_id",
                "added_by","added_datetime","updated_by","updated_datetime","synchronized");
        List<String> tables = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='ihms_equipment' ORDER BY 1")) {
            while (rs.next()) tables.add(rs.getString(1));
        }
        int bad = 0;
        for (String t : tables) {
            Set<String> cols = new HashSet<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_schema='ihms_equipment' AND table_name='" + t + "'")) {
                while (rs.next()) cols.add(rs.getString(1));
            }
            for (String a : audit) if (!cols.contains(a)) { System.out.println("MISSING " + a + " on " + t); bad++; }
        }
        System.out.println(bad == 0 ? "audit columns OK on all " + tables.size() + " tables" : bad + " AUDIT COLUMN GAPS");
        // seed spot-check + nextval smoke test
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT code FROM ihms_equipment.complaint_status_master ORDER BY id")) {
            StringBuilder sb = new StringBuilder("statuses:");
            while (rs.next()) sb.append(' ').append(rs.getString(1));
            System.out.println(sb);
        }
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT nextval('ihms_equipment.equipment_seq')")) {
            rs.next();
            System.out.println("equipment_seq nextval -> " + rs.getLong(1));
        }
    }

    static void check(Connection c, String label, String q, long expected) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(q)) {
            rs.next();
            long got = rs.getLong(1);
            System.out.println((got == expected ? "OK   " : "FAIL ") + label + ": " + got + (got == expected ? "" : " (expected " + expected + ")"));
        }
    }

    /** Quote-aware splitter: strips -- comments, splits on top-level ';'. */
    static List<String> split(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (inStr) {
                cur.append(ch);
                if (ch == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') { cur.append('\''); i++; }
                    else inStr = false;
                }
                continue;
            }
            if (ch == '\'') { inStr = true; cur.append(ch); continue; }
            if (ch == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') i++;
                cur.append('\n');
                continue;
            }
            if (ch == ';') { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(ch);
        }
        if (!cur.toString().isBlank()) out.add(cur.toString());
        return out;
    }
}
