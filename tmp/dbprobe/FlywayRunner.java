import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/** Runs the real Flyway runtime path against the shared PG, exactly like the service will on startup. */
public class FlywayRunner {
    public static void main(String[] args) {
        MigrateResult r = Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/ihms", "ihms", "ihms-dev-only")
                .defaultSchema("ihms_equipment")
                .schemas("ihms_equipment")
                .locations("filesystem:/mnt/d/hims_new_deloitte/services/equipment-service/src/main/resources/db/migration")
                .baselineOnMigrate(true)
                .createSchemas(true)
                .load()
                .migrate();
        System.out.println("migrationsApplied=" + r.migrationsExecuted);
        System.out.println("flyway=" + r.flywayVersion);
        for (Object m : r.migrations) System.out.println("applied: " + m);
        System.out.println("FLYWAY MIGRATE OK");
    }
}
