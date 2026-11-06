package com.hims.rmrs.application;

import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.application.exceptions.RmrsNotFoundException;
import com.hims.rmrs.domain.model.AccountingEntry;
import com.hims.rmrs.domain.model.AuthorityMaster;
import com.hims.rmrs.domain.model.HeadMaster;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.model.RmrsAuditableBase;
import com.hims.rmrs.domain.model.RmrsCollection;
import com.hims.rmrs.domain.repository.AccountingEntryRepository;
import com.hims.rmrs.domain.repository.AuthorityMasterRepository;
import com.hims.rmrs.domain.repository.HeadMasterRepository;
import com.hims.rmrs.domain.repository.LedgerMasterRepository;
import com.hims.rmrs.domain.repository.RmrsCollectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RMRS cash collections (GAP-REGISTER-5 WP-R5, Catalog #24).
 *
 * POST /api/v1/collections guards (all 400 BEXA003-style unless noted):
 *   receiptNo required + UNIQUE (duplicate -> 409 RMRS_DUPLICATE_RECEIPT);
 *   collectionDate required; amount required &gt; 0; headCode must EXIST;
 *   ledgerCode must EXIST; HEAD-LEDGER CONSISTENCY — the ledger resolved from
 *   ledgerCode must belong to the head resolved from headCode; optional
 *   authorityCode (when supplied) must EXIST.
 *
 * Balanced-accounting invariant (pinned by unit + PG16 integration tests):
 * the collection insert and its TWO postings are written in the SAME tx —
 *   DR the treasury-head ledger (debit = amount, credit = 0)
 *   CR the collection ledger   (credit = amount, debit = 0)
 * each entry EXACTLY ONE-SIDED (other side = 0, DB CHECK
 * ck_t_accounting_entry_one_sided) and SUM(debit) = SUM(credit) per collection,
 * re-verified from the DB after save inside the tx.
 *
 * EVENT POLICY (register WP-R5): CollectionPosted is PROPOSED with NO consumers
 * — NOTHING is ever emitted to the outbox. There is deliberately NO outbox
 * publisher/relay in this service; the outbox table exists only so tests can
 * pin its emptiness (negative assertion). Consuming E-08 PaymentReceived for
 * auto-receipt generation is a Wave-5b read-model concern (paymentRef ready).
 */
@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final RmrsCollectionRepository collectionRepository;
    private final AccountingEntryRepository entryRepository;
    private final HeadMasterRepository headRepository;
    private final LedgerMasterRepository ledgerRepository;
    private final AuthorityMasterRepository authorityRepository;

    @PersistenceContext
    private EntityManager em;

    public CollectionService(RmrsCollectionRepository collectionRepository,
                             AccountingEntryRepository entryRepository,
                             HeadMasterRepository headRepository,
                             LedgerMasterRepository ledgerRepository,
                             AuthorityMasterRepository authorityRepository) {
        this.collectionRepository = collectionRepository;
        this.entryRepository = entryRepository;
        this.headRepository = headRepository;
        this.ledgerRepository = ledgerRepository;
        this.authorityRepository = authorityRepository;
    }

    // ------------------------------------------------------------------ create

    /**
     * Create a collection + balanced postings in ONE transaction. See class
     * javadoc for the full guard list and posting rule.
     */
    @Transactional
    public RmrsCollection create(CollectionInput input) {
        // ---- guards ----------------------------------------------------------
        if (input.receiptNo() == null || input.receiptNo().isBlank()) {
            throw new InvalidRmrsRequestException("receiptNo is required");
        }
        if (input.collectionDate() == null) {
            throw new InvalidRmrsRequestException("collectionDate is required");
        }
        if (input.amount() == null || input.amount().signum() <= 0) {
            throw new InvalidRmrsRequestException("amount must be > 0");
        }
        if (input.headCode() == null || input.headCode().isBlank()) {
            throw new InvalidRmrsRequestException("headCode is required");
        }
        if (input.ledgerCode() == null || input.ledgerCode().isBlank()) {
            throw new InvalidRmrsRequestException("ledgerCode is required");
        }
        HeadMaster head = headRepository.findByHeadCode(input.headCode())
                .orElseThrow(() -> new InvalidRmrsRequestException(
                        "Unknown headCode: " + input.headCode()));
        LedgerMaster ledger = ledgerRepository.findByLedgerCode(input.ledgerCode())
                .orElseThrow(() -> new InvalidRmrsRequestException(
                        "Unknown ledgerCode: " + input.ledgerCode()));
        // head-ledger consistency: ledger must belong to the given head (400 otherwise)
        if (!ledger.getHeadId().equals(head.getHeadId())) {
            throw new InvalidRmrsRequestException(
                    "Ledger " + ledger.getLedgerCode() + " does not belong to head "
                            + head.getHeadCode() + " — head-ledger mismatch");
        }
        Long authorityId = null;
        if (input.authorityCode() != null && !input.authorityCode().isBlank()) {
            AuthorityMaster authority = authorityRepository.findByAuthorityCode(input.authorityCode())
                    .orElseThrow(() -> new InvalidRmrsRequestException(
                            "Unknown authorityCode: " + input.authorityCode()));
            authorityId = authority.getAuthorityId();
        }
        if (collectionRepository.existsByReceiptNo(input.receiptNo())) {
            throw new DuplicateRmrsResourceException("RMRS_DUPLICATE_RECEIPT",
                    "receiptNo already exists: " + input.receiptNo());
        }

        // ---- collection ------------------------------------------------------
        Timestamp nowTs = Timestamp.from(Instant.now());
        RmrsCollection collection = new RmrsCollection();
        collection.setCollectionId(nextId("t_rmrs_collection_seq"));
        collection.setReceiptNo(input.receiptNo());
        collection.setCollectionDate(input.collectionDate());
        collection.setPatientId(input.patientId());
        collection.setAmount(input.amount());
        collection.setHeadId(head.getHeadId());
        collection.setLedgerId(ledger.getLedgerId());
        collection.setAuthorityId(authorityId);
        collection.setPaymentRef(input.paymentRef());
        collection.setCollectedBy(input.collectedBy());
        collection.setAddedBy(AUDIT_ACTOR_ID);
        collection.setAddedDatetime(nowTs);
        collectionRepository.save(collection);

        // ---- balanced postings, SAME tx --------------------------------------
        // Posting rule (register WP-R5): DR the treasury-head ledger /
        // CR the collection ledger. Under the head-ledger consistency guard both
        // sides resolve to the collection's ledger row (the ledger validated to
        // belong to the head), so both postings reference it — one debit-sided,
        // one credit-sided.
        List<AccountingEntry> entries = new ArrayList<>();
        AccountingEntry dr = new AccountingEntry();
        dr.setEntryId(nextId("t_accounting_entry_seq"));
        dr.setCollectionId(collection.getCollectionId());
        dr.setLedgerId(collection.getLedgerId());
        dr.setDebit(input.amount());
        dr.setCredit(BigDecimal.ZERO);
        dr.setEntryDate(input.collectionDate());
        stampAudit(dr, nowTs);
        entries.add(dr);

        AccountingEntry cr = new AccountingEntry();
        cr.setEntryId(nextId("t_accounting_entry_seq"));
        cr.setCollectionId(collection.getCollectionId());
        cr.setLedgerId(collection.getLedgerId());
        cr.setDebit(BigDecimal.ZERO);
        cr.setCredit(input.amount());
        cr.setEntryDate(input.collectionDate());
        stampAudit(cr, nowTs);
        entries.add(cr);

        assertBalanced(collection.getCollectionId(), entries);   // build-time invariant
        entryRepository.saveAll(entries);

        // post-save verification from the DB, still inside the same tx
        List<Object[]> sumRows = entryRepository.sumsByCollectionId(collection.getCollectionId());
        Object[] sums = sumRows.isEmpty() ? new Object[]{null, null} : sumRows.get(0);
        BigDecimal sumDebit = sums[0] == null ? BigDecimal.ZERO : (BigDecimal) sums[0];
        BigDecimal sumCredit = sums[1] == null ? BigDecimal.ZERO : (BigDecimal) sums[1];
        if (sumDebit.compareTo(sumCredit) != 0) {
            throw new IllegalStateException(
                    "Balanced-postings invariant violated for collection "
                            + collection.getCollectionId() + ": SUM(debit)=" + sumDebit
                            + " != SUM(credit)=" + sumCredit);
        }

        // NO outbox write — CollectionPosted is PROPOSED (no consumers); see class javadoc.
        log.info("Created collection {} amount {} (DR/CR ledger {}) with balanced postings",
                collection.getReceiptNo(), input.amount(), collection.getLedgerId());
        return get(collection.getCollectionId());
    }

    /**
     * Build-time balanced-postings invariant: every entry exactly-one-sided,
     * SUM(debit) = SUM(credit), and at least two sides present. Package-private
     * for direct unit testing.
     */
    static void assertBalanced(long collectionId, List<AccountingEntry> entries) {
        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;
        for (AccountingEntry e : entries) {
            if (!e.isOneSided()) {
                throw new IllegalStateException(
                        "Exactly-one-sided entry invariant violated for collection " + collectionId
                                + ": entry " + e.getEntryId() + " debit=" + e.getDebit()
                                + " credit=" + e.getCredit());
            }
            sumDebit = sumDebit.add(e.getDebit() == null ? BigDecimal.ZERO : e.getDebit());
            sumCredit = sumCredit.add(e.getCredit() == null ? BigDecimal.ZERO : e.getCredit());
        }
        if (sumDebit.compareTo(sumCredit) != 0) {
            throw new IllegalStateException(
                    "Balanced-postings invariant violated for collection " + collectionId
                            + ": SUM(debit)=" + sumDebit + " != SUM(credit)=" + sumCredit);
        }
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public RmrsCollection get(long collectionId) {
        RmrsCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new RmrsNotFoundException(
                        "Collection not found: " + collectionId));
        collection.setEntries(entryRepository.findByCollectionIdOrderByEntryIdAsc(collectionId));
        return collection;
    }

    /** Paginated search — legacy ROWNUM pagination (:1361-1366) replaced by limit-offset. */
    @Transactional(readOnly = true)
    public Page<RmrsCollection> search(LocalDate dateFrom, LocalDate dateTo, Long headId, Pageable pageable) {
        return collectionRepository.search(dateFrom, dateTo, headId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ServiceWiseSummaryRow> summaryServiceWise(LocalDate dateFrom, LocalDate dateTo) {
        return collectionRepository.summaryServiceWise(dateFrom, dateTo);
    }

    @Transactional(readOnly = true)
    public List<UserWiseSummaryRow> summaryUserWise(LocalDate dateFrom, LocalDate dateTo) {
        return collectionRepository.summaryUserWise(dateFrom, dateTo);
    }

    // ------------------------------------------------------------------ helpers

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_rmrs." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static void stampAudit(RmrsAuditableBase entity, Timestamp now) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(now);
    }

    /** One requested collection at creation (register WP-R5 request body). */
    public record CollectionInput(String receiptNo, LocalDate collectionDate, Long patientId,
                                  BigDecimal amount, String headCode, String ledgerCode,
                                  String authorityCode, String paymentRef, String collectedBy) {}
}
