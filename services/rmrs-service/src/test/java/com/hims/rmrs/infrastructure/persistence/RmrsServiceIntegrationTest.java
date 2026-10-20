package com.hims.rmrs.infrastructure.persistence;

import com.hims.rmrs.RmrsServiceApplication;
import com.hims.rmrs.application.CollectionService;
import com.hims.rmrs.application.MasterService;
import com.hims.rmrs.application.ServiceWiseSummaryRow;
import com.hims.rmrs.application.UserWiseSummaryRow;
import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.domain.model.AccountingEntry;
import com.hims.rmrs.domain.model.RmrsCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-R5 RMRS & PMRD Treasury service on real PG16: V1 applies (ihms_rmrs),
 * masters CRUD-minimal, collection creation with BALANCED double-entry postings
 * in the same tx (SUM(debit)=SUM(credit) per collection, exactly-one-sided entries,
 * DB CHECK backstop), receipt uniqueness, summary math (s_servicewisecollection /
 * s_userwisecollection analogs), limit-offset pagination replacing legacy ROWNUM —
 * and the explicit NEGATIVE outbox assertion: CollectionPosted is PROPOSED (no
 * consumers) so NO outbox row is ever produced, on success OR on rejection.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = RmrsServiceApplication.class)
@DirtiesContext
@Transactional
class RmrsServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MasterService masterService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------ Flyway

    @Test
    void flywaySchema_applied_allTablesAndSequencesPresent() {
        // flyway_schema_history is Flyway's own bookkeeping table in this schema —
        // exclude it; we assert exactly the DOMAIN tables V1 must create.
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_rmrs'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """, String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "t_head_master", "t_authority_master", "t_ledger_master",
                "t_rmrs_collection", "t_accounting_entry", "outbox",
                "t_journal_voucher", "t_journal_voucher_leg",
                "t_bank_statement", "t_po_request");

        // exactly-one-sided CHECK is pinned at the DB level
        Integer oneSidedChecks = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conrelid = 'ihms_rmrs.t_accounting_entry'::regclass
                  AND contype = 'c' AND conname = 'ck_t_accounting_entry_one_sided'
                """, Integer.class);
        assertThat(oneSidedChecks).isEqualTo(1);

        // receipt uniqueness + amount>0 pinned at DB level
        Integer uniqueReceipt = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conrelid = 'ihms_rmrs.t_rmrs_collection'::regclass AND contype = 'u'
                  AND conname = 'uq_t_rmrs_collection_receipt'
                """, Integer.class);
        assertThat(uniqueReceipt).isEqualTo(1);
        Integer amountCheck = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conrelid = 'ihms_rmrs.t_rmrs_collection'::regclass AND contype = 'c'
                  AND conname = 'ck_t_rmrs_collection_amount'
                """, Integer.class);
        assertThat(amountCheck).isEqualTo(1);
    }

    // ------------------------------------------------------------------ full flow

    @Test
    void fullCollectionFlow_balancedPostings_andNegativeOutboxAssertion() {
        assertThat(outboxCount()).isZero();

        // masters (capture generated ids — sequences do NOT roll back with the
        // test tx, so hardcoding id=1 is order-dependent across tests)
        var head1 = masterService.createHead("PHARMACY", "Pharmacy cash head");
        var head2 = masterService.createHead("OPD", "OPD cash head");
        masterService.createAuthority("CASH-A", "Cash counter A", null);
        masterService.createLedger("PH-LEDGER", "Pharmacy ledger", head1.getHeadId());
        masterService.createLedger("OPD-LEDGER", "OPD ledger", head2.getHeadId());

        // guards BEFORE anything is written
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        null, LocalDate.of(2026, 2, 10), null, new BigDecimal("10"),
                        "PHARMACY", "PH-LEDGER", null, null, null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("receiptNo");
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-GUARD", LocalDate.of(2026, 2, 10), null, new BigDecimal("-1"),
                        "PHARMACY", "PH-LEDGER", null, null, null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("amount must be > 0");
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-GUARD", LocalDate.of(2026, 2, 10), null, new BigDecimal("10"),
                        "NOPE", "PH-LEDGER", null, null, null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown headCode");
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-GUARD", LocalDate.of(2026, 2, 10), null, new BigDecimal("10"),
                        "PHARMACY", "GHOST", null, null, null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown ledgerCode");
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-GUARD", LocalDate.of(2026, 2, 10), null, new BigDecimal("10"),
                        "PHARMACY", "OPD-LEDGER", null, null, null)))   // head-ledger mismatch
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("head-ledger mismatch");
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-GUARD", LocalDate.of(2026, 2, 10), null, new BigDecimal("10"),
                        "PHARMACY", "PH-LEDGER", "NOPE-A", null, null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown authorityCode");
        // every rejected create emitted NOTHING to the outbox
        assertThat(outboxCount()).isZero();
        assertThat(collectionCount()).isZero();

        // ---- valid create ------------------------------------------------------
        RmrsCollection c1 = collectionService.create(new CollectionService.CollectionInput(
                "RCP-0001", LocalDate.of(2026, 2, 10), 505L, new BigDecimal("100.00"),
                "PHARMACY", "PH-LEDGER", "CASH-A", "PAY-E08-1", "counter-staff-1"));
        long c1Id = c1.getCollectionId();

        assertThat(c1.getEntries()).hasSize(2);
        AccountingEntry dr = c1.getEntries().get(0);
        AccountingEntry cr = c1.getEntries().get(1);
        assertThat(dr.getDebit()).isEqualByComparingTo("100.00");
        assertThat(dr.getCredit()).isEqualByComparingTo("0");
        assertThat(cr.getDebit()).isEqualByComparingTo("0");
        assertThat(cr.getCredit()).isEqualByComparingTo("100.00");

        // DB-level balance verification for the collection (SUM(debit)=SUM(credit))
        assertBalancedInDb(c1Id);

        // paymentRef column carries the E-08 linkage string
        String paymentRef = jdbcTemplate.queryForObject(
                "SELECT payment_ref FROM ihms_rmrs.t_rmrs_collection WHERE collection_id = ?",
                String.class, c1Id);
        assertThat(paymentRef).isEqualTo("PAY-E08-1");

        // ---- NEGATIVE outbox assertion: successful creates produce NO row ------
        assertThat(outboxCount()).as("CollectionPosted is PROPOSED — no outbox rows").isZero();

        // ---- receipt uniqueness: 409, nothing written --------------------------
        assertThatThrownBy(() -> collectionService.create(new CollectionService.CollectionInput(
                        "RCP-0001", LocalDate.of(2026, 2, 11), null, new BigDecimal("42"),
                        "PHARMACY", "PH-LEDGER", null, null, null)))
                .isInstanceOfSatisfying(DuplicateRmrsResourceException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("RMRS_DUPLICATE_RECEIPT"));
        assertThat(outboxCount()).as("rejected duplicate emits nothing").isZero();
        assertThat(collectionCount()).isEqualTo(1);
    }

    @Test
    void dbCheck_enforcesAmountPositive() {
        var head = masterService.createHead("PHARMACY", "Pharmacy cash head");
        var ledger = masterService.createLedger("PH-LEDGER", "Pharmacy ledger", head.getHeadId());

        // amount > 0 CHECK at DB level (bypassing the service guard natively).
        // Constraint failure aborts the PG tx — keep it the LAST statement; the
        // Spring test framework rolls the test tx back regardless.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO ihms_rmrs.t_rmrs_collection
                    (collection_id, receipt_no, collection_date, amount, head_id, ledger_id)
                VALUES (nextval('ihms_rmrs.t_rmrs_collection_seq'), 'RCP-BAD',
                        DATE '2026-02-10', -1.00, ?, ?)
                """, head.getHeadId(), ledger.getLedgerId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void dbCheck_enforcesExactlyOneSidedEntries() {
        var head = masterService.createHead("PHARMACY", "Pharmacy cash head");
        masterService.createLedger("PH-LEDGER", "Pharmacy ledger", head.getHeadId());
        RmrsCollection c = collectionService.create(new CollectionService.CollectionInput(
                "RCP-CHK", LocalDate.of(2026, 2, 10), null, new BigDecimal("5.00"),
                "PHARMACY", "PH-LEDGER", null, null, null));

        long debitEntryId = c.getEntries().stream()
                .filter(e -> e.getDebit().signum() > 0).findFirst().orElseThrow().getEntryId();
        // making an entry two-sided violates ck_t_accounting_entry_one_sided
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE ihms_rmrs.t_accounting_entry SET credit = 5 WHERE entry_id = ?
                """, debitEntryId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ summary math

    @Test
    void summaryMath_serviceWise_andUserWise_matchCollections() {
        var pharmacyHead = masterService.createHead("PHARMACY", "Pharmacy cash head");
        var opd = masterService.createHead("OPD", "OPD cash head");
        masterService.createLedger("PH-LEDGER", "Pharmacy ledger", pharmacyHead.getHeadId());
        masterService.createLedger("OPD-LEDGER", "OPD ledger", opd.getHeadId());

        collectionService.create(new CollectionService.CollectionInput(
                "RCP-A", LocalDate.of(2026, 2, 10), null, new BigDecimal("100.00"),
                "PHARMACY", "PH-LEDGER", null, null, "alice"));
        collectionService.create(new CollectionService.CollectionInput(
                "RCP-B", LocalDate.of(2026, 2, 15), null, new BigDecimal("50.50"),
                "PHARMACY", "PH-LEDGER", null, null, "bob"));
        collectionService.create(new CollectionService.CollectionInput(
                "RCP-C", LocalDate.of(2026, 3, 1), null, new BigDecimal("200.00"),
                "OPD", "OPD-LEDGER", null, null, "bob"));

        // service-wise totals across ALL dates: PHARMACY=150.50 x2, OPD=200.00 x1
        List<ServiceWiseSummaryRow> all = collectionService.summaryServiceWise(null, null);
        assertThat(all).hasSize(2);
        ServiceWiseSummaryRow pharmacy = byHead(all, "PHARMACY");
        assertThat(pharmacy.totalAmount()).isEqualByComparingTo("150.50");
        assertThat(pharmacy.collectionCount()).isEqualTo(2L);
        ServiceWiseSummaryRow opdRow = byHead(all, "OPD");
        assertThat(opdRow.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(opdRow.collectionCount()).isEqualTo(1L);

        // date filter excludes March
        List<ServiceWiseSummaryRow> feb = collectionService.summaryServiceWise(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(feb).hasSize(1);
        assertThat(feb.get(0).headCode()).isEqualTo("PHARMACY");
        assertThat(feb.get(0).totalAmount()).isEqualByComparingTo("150.50");

        // user-wise: alice=100.00 x1, bob=250.50 x2
        List<UserWiseSummaryRow> users = collectionService.summaryUserWise(null, null);
        assertThat(users).hasSize(2);
        UserWiseSummaryRow alice = users.stream()
                .filter(u -> "alice".equals(u.collectedBy())).findFirst().orElseThrow();
        UserWiseSummaryRow bob = users.stream()
                .filter(u -> "bob".equals(u.collectedBy())).findFirst().orElseThrow();
        assertThat(alice.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(alice.collectionCount()).isEqualTo(1L);
        assertThat(bob.totalAmount()).isEqualByComparingTo("250.50");
        assertThat(bob.collectionCount()).isEqualTo(2L);

        // summaries never touch the outbox either
        assertThat(outboxCount()).isZero();
    }

    // ------------------------------------------------------------------ pagination

    @Test
    void listSearch_limitOffsetPagination_replacesLegacyRownum() {
        var head = masterService.createHead("PHARMACY", "Pharmacy cash head");
        masterService.createLedger("PH-LEDGER", "Pharmacy ledger", head.getHeadId());
        for (int i = 1; i <= 5; i++) {
            collectionService.create(new CollectionService.CollectionInput(
                    "RCP-P" + i, LocalDate.of(2026, 2, i), null, new BigDecimal(i + ".00"),
                    "PHARMACY", "PH-LEDGER", null, null, "staff"));
        }
        Page<RmrsCollection> page0 =
                collectionService.search(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                        head.getHeadId(), PageRequest.of(0, 2));
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalPages()).isEqualTo(3);

        Page<RmrsCollection> page2 =
                collectionService.search(null, null, null, PageRequest.of(2, 2));
        assertThat(page2.getContent()).hasSize(1);   // last page carries the remainder

        // headId filter
        Page<RmrsCollection> filtered =
                collectionService.search(null, null, 99L, PageRequest.of(0, 20));
        assertThat(filtered.getTotalElements()).isZero();

        assertThat(outboxCount())
                .as("pagination reads emit nothing; CollectionPosted stays PROPOSED")
                .isZero();
    }

    // ------------------------------------------------------------------ helpers

    private static ServiceWiseSummaryRow byHead(List<ServiceWiseSummaryRow> rows, String code) {
        return rows.stream().filter(r -> code.equals(r.headCode())).findFirst().orElseThrow();
    }

    private long outboxCount() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_rmrs.outbox", Long.class);
        return n == null ? -1 : n;
    }

    private long collectionCount() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_rmrs.t_rmrs_collection", Long.class);
        return n == null ? -1 : n;
    }

    /** SUM(debit) = SUM(credit) per collection, read from the DB. */
    private void assertBalancedInDb(long collectionId) {
        var sums = jdbcTemplate.queryForMap("""
                SELECT SUM(debit) AS d, SUM(credit) AS c, COUNT(*) AS n
                FROM ihms_rmrs.t_accounting_entry WHERE collection_id = ?
                """, collectionId);
        BigDecimal sumDebit = (BigDecimal) sums.get("d");
        BigDecimal sumCredit = (BigDecimal) sums.get("c");
        assertThat(sumDebit).isEqualByComparingTo(sumCredit);
        assertThat(((Number) sums.get("n")).longValue()).isEqualTo(2L);
    }
}
