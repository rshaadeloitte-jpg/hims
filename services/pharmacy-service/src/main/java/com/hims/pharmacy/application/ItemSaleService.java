package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.repository.ItemSaleDetailRepository;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pharmacy &amp; Dispensing slice (GAP-REGISTER-5 WP-R2, Catalog #9), schema
 * ihms_pharmacy.
 *
 * Status machine (t_itemsale.status_code SMALLINT):
 *   1=PENDING -> partial dispense -> 2=PARTIALLY_DISPENSED
 *             -> ALL lines dispensed -> 3=FULLY_DISPENSED (terminal).
 *
 * BR-PHAR-001 (pinned by tests):
 *   each dispensed detail gets dispensed=true when its pending_qty reaches 0;
 *   partial dispenses keep the sale PARTIALLY_DISPENSED and decrement pending_qty;
 *   when ALL details are dispensed the header flag dispensed=true and status
 *   FULLY_DISPENSED are set in the same transaction.
 *
 * BR-PHAR-002 (pinned by tests, see {@link PrescriptionDispenseService}):
 *   full-qty sale line closes the linked prescription projection detail
 *   (IsClosed='1' PendingQty=0); partial decrements PendingQty; the master closes
 *   when all its details are closed. The write-back into ordering-service's
 *   ihms_clinical is DEFERRED post-saga — this service only owns its local
 *   projection.
 *
 * STOCK STAYS IN item-service (single-writer D-1): every dispensed qty POSTs a
 * SUBTRACT movement to item-service synchronously ({@link ItemStockClient}).
 * On ANY stock failure the whole dispense transaction rolls back: the sale stays
 * PENDING, nothing is marked dispensed.
 *
 * EVENTS: DispensingCompleted is PROPOSED per GAP-REGISTER-5 WP-R2 (no
 * multi-consumer evidence — NOT in KAFKA-EVENT-CATALOG) — NOTHING is emitted to
 * the outbox. Consuming PrescriptionCreated / InventoryUpdated post-saga is a
 * comment-only concern here. This structural no-outbox guarantee is asserted by
 * the PG16 integration test (outbox row count stays 0 across every flow).
 */
@Service
public class ItemSaleService {

    private static final Logger log = LoggerFactory.getLogger(ItemSaleService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final ItemSaleRepository saleRepository;
    private final ItemSaleDetailRepository detailRepository;
    private final ItemStockClient stockClient;
    private final PrescriptionDispenseService prescriptionDispenseService;
    private final DrugAllergyService allergyService;
    private final DrugInteractionService interactionService;
    private final long pharmacyStoreId;

    @PersistenceContext
    private EntityManager em;

    public ItemSaleService(ItemSaleRepository saleRepository,
                           ItemSaleDetailRepository detailRepository,
                           ItemStockClient stockClient,
                           PrescriptionDispenseService prescriptionDispenseService,
                           DrugAllergyService allergyService,
                           DrugInteractionService interactionService,
                           @Value("${app.pharmacy.store-id:1}") long pharmacyStoreId) {
        this.saleRepository = saleRepository;
        this.detailRepository = detailRepository;
        this.stockClient = stockClient;
        this.prescriptionDispenseService = prescriptionDispenseService;
        this.allergyService = allergyService;
        this.interactionService = interactionService;
        this.pharmacyStoreId = pharmacyStoreId;
    }

    // ------------------------------------------------------------------ create

    /**
     * Create an item sale (status PENDING). Guards (400 BEXA003-style):
     * patientId required; at least ONE line required; every line needs itemId and
     * qty > 0 (integer units). Stock is NOT touched here — it is borrowed from
     * item-service at dispense time. Emits NOTHING to the outbox.
     */
    @Transactional
    public ItemSale create(Long patientId, Long prescriptionId, List<LineInput> lines) {
        if (patientId == null) {
            throw new InvalidSaleRequestException("BEXA003", "patientId is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidSaleRequestException("BEXA003",
                    "At least one sale line {itemId,batchId,qty} is required");
        }
        for (LineInput line : lines) {
            if (line == null || line.itemId() == null) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Each line needs a non-null itemId");
            }
            if (line.qty() == null || line.qty() <= 0) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Line qty must be > 0 for itemId " + line.itemId());
            }
        }

        Timestamp nowTs = now();
        ItemSale sale = new ItemSale();
        sale.setSaleId(nextId("t_itemsale_seq"));
        sale.setPatientId(patientId);
        sale.setPrescriptionId(prescriptionId);
        sale.setStatusCode(ItemSale.STATUS_PENDING);
        sale.setDispensed(false);
        sale.setAddedBy(AUDIT_ACTOR_ID);
        sale.setAddedDatetime(nowTs);
        saleRepository.save(sale);

        // Pre-dispense gates: allergies & interactions
        allergyService.checkDispense(patientId, lines);
        interactionService.checkDispense(patientId, lines);

        List<ItemSaleDetail> savedLines = new ArrayList<>();
        long lineNo = 1L;
        for (LineInput line : lines) {
            ItemSaleDetail d = new ItemSaleDetail();
            d.setSaleId(sale.getSaleId());
            d.setLineNo(lineNo++);
            d.setItemId(line.itemId());
            d.setBatchId(line.batchId());
            d.setQty(line.qty());
            d.setPendingQty(line.qty());          // nothing dispensed yet
            d.setDispensed(false);
            d.setVerified(false);
            d.setPartialDispense(false);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(nowTs);
            detailRepository.save(d);
            savedLines.add(d);
        }

        if (prescriptionId != null) {
            // BR-PHAR-002 local projection seed (ordering-service keeps truth via E-06)
            prescriptionDispenseService.projectSaleLines(prescriptionId, savedLines);
        }

        log.info("Created item sale {} for patient {} with {} line(s){}",
                sale.getSaleId(), patientId, savedLines.size(),
                prescriptionId == null ? "" : " linked to prescription " + prescriptionId);
        return get(sale.getSaleId());
    }

    // ------------------------------------------------------------------ dispense

    /**
     * Dispense (fully or partially) the requested lines of a sale.
     *
     * Guards: unknown sale -> 404; already FULLY_DISPENSED -> 400
     * PH_ILLEGAL_TRANSITION; empty request -> 400; unknown line_no -> 400;
     * qty <= 0 or qty > remaining pending_qty -> 400 (qty == pending is the
     * closing boundary and is allowed).
     *
     * Per line: synchronous SUBTRACT movement on item-service (any failure rolls
     * back everything), then BR-PHAR-001 detail/header updates and — when the sale
     * carries a prescriptionId — BR-PHAR-002 projection updates.
     */
    @Transactional
    public ItemSale dispense(long saleId, List<DispenseLine> requested) {
        ItemSale sale = load(saleId);
        if (sale.isFullyDispensed()) {
            throw new IllegalSaleStateException("PH_ILLEGAL_TRANSITION",
                    "Cannot dispense sale " + saleId + " from status FULLY_DISPENSED");
        }
        if (requested == null || requested.isEmpty()) {
            throw new InvalidSaleRequestException("BEXA003",
                    "At least one dispense line {lineNo,qty} is required");
        }

        List<ItemSaleDetail> details = detailsOf(saleId);
        for (DispenseLine req : requested) {
            if (req == null || req.lineNo() == null) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Each dispense line needs lineNo");
            }
            ItemSaleDetail detail = details.stream()
                    .filter(d -> d.getLineNo().equals(req.lineNo()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidSaleRequestException("BEXA003",
                            "Unknown lineNo " + req.lineNo() + " for sale " + saleId));
            if (req.qty() == null || req.qty() <= 0) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Dispense qty must be > 0 for lineNo " + req.lineNo());
            }
            if (req.qty() > detail.getPendingQty()) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Dispense qty " + req.qty() + " exceeds pending_qty "
                                + detail.getPendingQty() + " for lineNo " + req.lineNo()
                                + " of sale " + saleId);
            }
        }

        // deterministic order; validation completes BEFORE any mutation/stock call
        List<DispenseLine> ordered = new ArrayList<>(requested);
        ordered.sort(Comparator.comparing(DispenseLine::lineNo));

        for (DispenseLine req : ordered) {
            ItemSaleDetail detail = details.stream()
                    .filter(d -> d.getLineNo().equals(req.lineNo()))
                    .findFirst().orElseThrow();   // validated above

            // single-writer borrow: item-service decrements ledger+balance.
            // Any failure throws StockIssueException -> whole tx rolls back.
            stockClient.issueForSale(pharmacyStoreId, detail.getItemId(),
                    detail.getBatchId(), req.qty(), saleId, req.lineNo());

            boolean fullyDispensed = req.qty().intValue() == detail.getPendingQty();
            if (fullyDispensed) {
                detail.setDispensed(true);                       // BR-PHAR-001 detail flag
                detail.setPendingQty(0);
            } else {
                detail.setPendingQty(detail.getPendingQty() - req.qty());   // partial decrement
                detail.setPartialDispense(true);
            }
            detail.setUpdatedBy(AUDIT_ACTOR_ID);
            detail.setUpdatedDatetime(now());
            detailRepository.save(detail);

            if (sale.getPrescriptionId() != null) {
                // BR-PHAR-002 local projection: full-qty closes, partial decrements
                prescriptionDispenseService.applyDispenseOutcome(
                        sale.getPrescriptionId(), req.lineNo(), fullyDispensed, req.qty());
            }
        }

        // BR-PHAR-001 header rule: recompute over ALL details
        boolean allDispensed = details.stream().allMatch(ItemSaleDetail::getDispensed);
        if (allDispensed) {
            sale.setDispensed(true);
            sale.setStatusCode(ItemSale.STATUS_FULLY_DISPENSED);
        } else {
            sale.setStatusCode(ItemSale.STATUS_PARTIALLY_DISPENSED);
        }
        sale.setUpdatedBy(AUDIT_ACTOR_ID);
        sale.setUpdatedDatetime(now());
        saleRepository.save(sale);
        log.info("Dispensed {} line request(s) on sale {} -> status {}",
                requested.size(), saleId, sale.getStatusCode());

        // NOTE: intentionally NO outbox append anywhere in this method —
        // DispensingCompleted is PROPOSED (not in the event catalog); the PG16
        // integration test asserts the outbox table stays EMPTY after every flow.
        return get(saleId);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public ItemSale get(long saleId) {
        ItemSale sale = load(saleId);
        sale.setDetails(detailsOf(saleId));
        return sale;
    }

    @Transactional(readOnly = true)
    public List<ItemSale> list(Short status, Long patientId) {
        List<ItemSale> sales = saleRepository.search(status, patientId);
        for (ItemSale s : sales) {
            s.setDetails(detailsOf(s.getSaleId()));
        }
        return sales;
    }

    // ------------------------------------------------------------------ helpers

    private ItemSale load(long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND",
                        "Item sale not found: " + saleId));
    }

    private List<ItemSaleDetail> detailsOf(long saleId) {
        return detailRepository.findBySaleIdOrderByLineNoAsc(saleId);
    }

    static String statusName(short s) {
        return switch (s) {
            case 1 -> "PENDING";
            case 2 -> "PARTIALLY_DISPENSED";
            case 3 -> "FULLY_DISPENSED";
            default -> String.valueOf(s);
        };
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy." + sequenceName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested sale line at creation. */
    public record LineInput(Long itemId, String batchId, Integer qty) {}

    /** One requested dispense amount against a sale line. */
    public record DispenseLine(Long lineNo, Integer qty) {}
}
