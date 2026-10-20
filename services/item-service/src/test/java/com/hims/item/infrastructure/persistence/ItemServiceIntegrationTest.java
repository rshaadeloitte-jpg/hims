package com.hims.item.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.ItemServiceApplication;
import com.hims.item.application.ItemMasterService;
import com.hims.item.application.StockService;
import com.hims.item.domain.model.Item;
import com.hims.item.domain.model.Grn;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.model.ItemStockLedger;
import com.hims.item.domain.model.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-012 item service on real PG16: V1 schema applies (ihms_item + outbox),
 * item/store/batch masters via sequences, and the D-1 single-writer stock flow
 * (movement -> balance + ledger + outbox row in one tx).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ItemServiceApplication.class)
@DirtiesContext
@Transactional
class ItemServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ItemMasterService masterService;

    @Autowired
    private StockService stockService;

    @Autowired
    private com.hims.item.application.SupplierService supplierService;

    @Autowired
    private com.hims.item.application.GrnService grnService;

    @Autowired
    private com.hims.item.application.IndentService indentService;

    @Autowired
    private com.hims.item.application.ItemEnquiryService enquiryService;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void advanceSequences() {
        // setval is non-transactional in PG: survives the test-tx rollback, so each
        // test deterministically starts ids at 1001/2001/3001.
        em.createNativeQuery("SELECT setval('ihms_item.\"item_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_item.\"store_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_item.\"item_batch_seq\"', 3000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation + seed item_group id=1 (item FK target) in a committed
        // tx so it is visible to the REQUIRES_NEW service writes.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_item.t_grn_detail, ihms_item.t_grn,
                               ihms_item.t_indent_detail, ihms_item.t_indent,
                               ihms_item.supplier, ihms_item.item_stock_ledger,
                               ihms_item.item_stock_balance, ihms_item.item_batch,
                               ihms_item.item, ihms_item.store, ihms_item.outbox CASCADE""").executeUpdate();
            Number existing = (Number) em.createNativeQuery(
                    "SELECT count(*) FROM ihms_item.item_group WHERE id = 1").getSingleResult();
            if (existing.longValue() == 0) {
                em.createNativeQuery("""
                        INSERT INTO ihms_item.item_group (id, unit_id, status, code, description, synchronized)
                        VALUES (1, 7, TRUE, 'PHARMA', 'Pharmaceuticals', FALSE)""").executeUpdate();
            }
        });
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_item' AND table_name IN
                        ('item','store','item_batch','item_stock_balance','item_stock_ledger','outbox')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).contains("item", "store", "item_batch", "item_stock_balance", "item_stock_ledger", "outbox");
        List<String> partial = em.createNativeQuery("""
                        SELECT indexname FROM pg_indexes
                        WHERE schemaname = 'ihms_item' AND indexname = 'uq_item_stock_balance_no_batch'""")
                .getResultList();
        assertThat(partial).hasSize(1);
    }

    @Test
    void itemStoreBatch_masters_create() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemCode\":\"PCM500\",\"itemName\":\"Paracetamol 500mg\",\"itemGroupId\":1}"), 7L);
        assertThat(item.getId()).isEqualTo(1001L);
        assertThat(item.getUnitId()).isEqualTo(7L);
        assertThat(item.getStatus()).isTrue();

        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"MAIN\",\"description\":\"Main Pharmacy Store\"}"), 7L);
        assertThat(store.getId()).isEqualTo(2001L);

        com.hims.item.domain.model.ItemBatch batch = masterService.createBatch(
                mapper.readTree("{\"itemId\":1001,\"batchCode\":\"B-2026-01\",\"expiryDate\":\"2028-01-31\",\"quantity\":1000}"), 7L);
        assertThat(batch.getId()).isEqualTo(3001L);
        assertThat(batch.getBatchCode()).isEqualTo("B-2026-01");
    }

    @Test
    void stockMovement_singleWriterFlow() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Amoxicillin 250mg\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"PH1\",\"description\":\"Pharmacy 1\"}"), 7L);

        // opening + GRN (add)
        ItemStockBalance b1 = stockService.postMovement(store.getId(), item.getId(), null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(500), "OPENING", null, "opening");
        ItemStockBalance b2 = stockService.postMovement(store.getId(), item.getId(), null, null,
                (short) 2, StockService.OP_ADD, BigDecimal.valueOf(200), "GRN", 77L, "purchase");

        assertThat(b1.getId()).isEqualTo(b2.getId());           // same balance row reused (single non-batch row)
        assertThat(b2.getQtyInHand()).isEqualByComparingTo("700");
        assertThat(b2.getVersion()).isEqualTo(2L);

        // issue (subtract)
        ItemStockBalance b3 = stockService.postMovement(store.getId(), item.getId(), null, null,
                (short) 3, StockService.OP_SUBTRACT, BigDecimal.valueOf(120), "ISSUE", 11L, "ward issue");
        assertThat(b3.getQtyInHand()).isEqualByComparingTo("580");

        // ledger has 3 rows with correct prev/new balances
        List<ItemStockLedger> ledger = stockService.getLedger(store.getId(), item.getId());
        assertThat(ledger).hasSize(3);
        assertThat(ledger.get(2).getPrevBalance()).isEqualByComparingTo("0");
        assertThat(ledger.get(2).getNewBalance()).isEqualByComparingTo("500");
        assertThat(ledger.get(0).getNewBalance()).isEqualByComparingTo("580");

        // one outbox row per movement, appended in the same tx (3 movements -> 3 events)
        Number outboxRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_item.outbox WHERE event_type = 'InventoryUpdated' AND aggregate_id = "
                                + b3.getId())
                .getSingleResult();
        assertThat(outboxRows.longValue()).isEqualTo(3L);
    }

    @Test
    void availableBatches_fefoOrderingAndFilters() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Ceftriaxone 1g\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"PH3\",\"description\":\"Pharmacy 3\"}"), 7L);

        // batches: far expiry, near expiry, null expiry, expired, freeze-recalled, zero stock
        com.hims.item.domain.model.ItemBatch far = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"FAR\",\"expiryDate\":\"2028-12-31\"}"), 7L);
        com.hims.item.domain.model.ItemBatch near = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"NEAR\",\"expiryDate\":\"2027-01-31\"}"), 7L);
        com.hims.item.domain.model.ItemBatch noExp = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"NOEXP\"}"), 7L);
        com.hims.item.domain.model.ItemBatch expired = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"EXP\",\"expiryDate\":\"2020-01-01\"}"), 7L);
        com.hims.item.domain.model.ItemBatch recalled = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"RECALL\",\"expiryDate\":\"2028-06-30\",\"isFreezeRecall\":true}"), 7L);
        com.hims.item.domain.model.ItemBatch empty = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"EMPTY\",\"expiryDate\":\"2028-06-30\"}"), 7L);

        // stock each batch (opening balance, movementType 1)
        for (com.hims.item.domain.model.ItemBatch b : List.of(far, near, noExp, expired, recalled)) {
            stockService.postMovement(store.getId(), item.getId(), b.getId(), null,
                    (short) 1, StockService.OP_ADD, BigDecimal.valueOf(100), "OPENING", null, null);
        }
        // 'empty' gets +100 then -100 -> available 0
        stockService.postMovement(store.getId(), item.getId(), empty.getId(), null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(100), "OPENING", null, null);
        stockService.postMovement(store.getId(), item.getId(), empty.getId(), null,
                (short) 3, StockService.OP_SUBTRACT, BigDecimal.valueOf(100), "ISSUE", null, null);

        List<com.hims.item.application.BatchAvailabilityView> view =
                stockService.getAvailableBatches(store.getId(), item.getId());

        // FEFO: near expiry first, then far, then null-expiry last; expired / recalled / zero excluded
        assertThat(view).extracting(com.hims.item.application.BatchAvailabilityView::batchCode)
                .containsExactly("NEAR", "FAR", "NOEXP");
        assertThat(view.get(0).availableQty()).isEqualByComparingTo("100");
        assertThat(view.get(0).itemName()).isEqualTo("Ceftriaxone 1g");
    }

    @Test
    void availableBatches_requiresStoreAndItem() throws Exception {
        assertThatThrownBy(() -> stockService.getAvailableBatches(null, 1L))
                .isInstanceOf(com.hims.item.application.exceptions.InvalidItemRequestException.class);
        assertThatThrownBy(() -> stockService.getAvailableBatches(1L, 99999L))
                .isInstanceOf(com.hims.item.application.exceptions.ItemNotFoundException.class);
    }

    @Test
    void stockMovement_rollsBackOnMissingItem() throws Exception {
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"PH2\",\"description\":\"Pharmacy 2\"}"), 7L);
        assertThatThrownBy(() -> stockService.postMovement(store.getId(), 99999L, null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.TEN, null, null, null))
                .isInstanceOf(com.hims.item.application.exceptions.ItemNotFoundException.class);
        Number ledgerRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_item.item_stock_ledger WHERE store_id = " + store.getId())
                .getSingleResult();
        assertThat(ledgerRows.longValue()).isZero();
    }

    // ------------------------------------------------------------- WP-2 procurement slice

    @Test
    void v2ProcurementSchema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_item' AND table_name IN
                        ('supplier','t_grn','t_grn_detail','t_indent','t_indent_detail')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).contains("supplier", "t_grn", "t_grn_detail", "t_indent", "t_indent_detail");
        List<String> seqs = em.createNativeQuery("""
                        SELECT sequencename FROM pg_sequences
                        WHERE schemaname = 'ihms_item' AND sequencename IN
                        ('t_grn_seq','t_grn_detail_seq','t_indent_seq','t_indent_detail_seq')
                        ORDER BY sequencename""").getResultList();
        assertThat(seqs).hasSize(4);
    }

    @Test
    void grnPost_oneTx_ledgerBalanceBatches_andOutboxEvents() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Ceftriaxone 1g\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"PHG\",\"description\":\"Pharmacy GRN\"}"), 7L);
        com.hims.item.domain.model.Supplier supplier = supplierService.create(
                mapper.readTree("{\"code\":\"SUP-01\",\"description\":\"Acme Pharma\"}"));

        String body = """
                {"storeId":%d,"supplierId":%d,"invoiceNo":"INV-42",
                 "lines":[
                   {"itemId":%d,"quantity":100,"batchCode":"B-GRN-1","expiryDate":"2028-06-30","mrp":25.50},
                   {"itemId":%d,"quantity":40,"batchCode":"B-GRN-2"}
                 ]}
                """.formatted(store.getId(), supplier.getId(), item.getId(), item.getId());

        Grn grn = grnService.postGrn(mapper.readTree(body));

        assertThat(grn.getGrnNo()).startsWith("GRN-");
        assertThat(grn.getDocStatus()).isEqualTo(Grn.STATUS_PENDING);

        // both lines moved stock through the D-1 path (balance updated, batches created inline)
        var balances = stockService.getBalances(store.getId(), item.getId());
        assertThat(balances).hasSize(2);   // two distinct batches -> two balance rows
        var total = balances.stream().map(ItemStockBalance::getQtyInHand)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("140");

        // ledger rows reference the GRN header
        var ledger = stockService.getLedger(store.getId(), item.getId());
        assertThat(ledger).hasSize(2);
        assertThat(ledger).allSatisfy(l -> {
            assertThat(l.getRefDocType()).isEqualTo("GRN");
            assertThat(l.getRefDocId()).isEqualTo(grn.getId());
            assertThat(l.getMovementType()).isEqualTo((short) 2);
        });

        // outbox: one InventoryUpdated per line + one aggregate GRNPosted
        Number inv = (Number) em.createNativeQuery(
                "SELECT count(*) FROM ihms_item.outbox WHERE event_type = 'InventoryUpdated'").getSingleResult();
        assertThat(inv.longValue()).isEqualTo(2);
        Number posted = (Number) em.createNativeQuery("""
                SELECT count(*) FROM ihms_item.outbox
                WHERE event_type = 'GRNPosted' AND aggregate_id = :gid""").setParameter("gid", grn.getId())
                .getSingleResult();
        assertThat(posted.longValue()).isEqualTo(1);

        // grn details persisted
        Number details = (Number) em.createNativeQuery(
                "SELECT count(*) FROM ihms_item.t_grn_detail WHERE grn_id = :gid")
                .setParameter("gid", grn.getId()).getSingleResult();
        assertThat(details.longValue()).isEqualTo(2);
    }

    @Test
    void indent_createThenApprove_emitsIndentApproved_once() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Gloves M\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"IND1\",\"description\":\"Ward Store\"}"), 7L);

        String body = """
                {"storeId":%d,"lines":[{"itemId":%d,"quantity":25}]}
                """.formatted(store.getId(), item.getId());
        com.hims.item.domain.model.Indent indent = indentService.create(mapper.readTree(body));
        assertThat(indent.getDocStatus()).isEqualTo(com.hims.item.domain.model.Indent.STATUS_PENDING);

        var approved = indentService.approve(indent.getId());
        assertThat(approved.getDocStatus()).isEqualTo(com.hims.item.domain.model.Indent.STATUS_APPROVED);
        assertThat(approved.getApprovedDatetime()).isNotNull();

        Number events = (Number) em.createNativeQuery("""
                SELECT count(*) FROM ihms_item.outbox
                WHERE event_type = 'IndentApproved' AND aggregate_id = :iid""")
                .setParameter("iid", indent.getId()).getSingleResult();
        assertThat(events.longValue()).isEqualTo(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> indentService.approve(indent.getId()))
                .isInstanceOf(com.hims.item.application.exceptions.InvalidItemRequestException.class);

        assertThat(indentService.list((short) 2)).extracting("id").contains(indent.getId());
    }

    // ------------------------------------------------------------- M13 item enquiry (V3 read-side)

    @Test
    void itemEnquiry_search_paginatesByName_withTrigramOrdering() throws Exception {
        Item a = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Paracetamol 500mg EQ\"}"), 7L);
        Item b = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Paracetamol 250mg EQ\"}"), 7L);
        Item c = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Amoxicillin 250mg EQ\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"ENQ1\",\"description\":\"Enquiry Store\"}"), 7L);
        // opening stock for the two paracetamol items in the store
        stockService.postMovement(store.getId(), a.getId(), null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(100), "OPENING", null, null);
        stockService.postMovement(store.getId(), b.getId(), null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(20), "OPENING", null, null);

        // search "paracetamol" -> 2 hits, ordered by similarity + name
        var rows = enquiryService.search("paracetamol", null, true, 0, 50);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting("itemId").containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.totalRows()).isEqualTo(2L);
            assertThat(r.storeId()).isNull();
            assertThat(r.availableQty()).isNull();
        });

        // includeZeroStock=false drops the amoxicillin item (no stock in this store)
        var storeRows = enquiryService.search("amoxicillin", store.getId(), false, 0, 50);
        assertThat(storeRows).isEmpty();
        // includeZeroStock=true keeps both paracetamol items with their stock qty
        var stockRows = enquiryService.search("paracetamol", store.getId(), true, 0, 50);
        assertThat(stockRows).extracting("availableQty")
                .usingElementComparator((x, y) -> {
                    if (x == null && y == null) return 0;
                    if (x == null) return -1;
                    if (y == null) return 1;
                    return ((java.math.BigDecimal) x).compareTo((java.math.BigDecimal) y);
                })
                .containsExactlyInAnyOrder(BigDecimal.valueOf(100), BigDecimal.valueOf(20));
    }

    @Test
    void itemEnquiry_batches_reusesStockServiceFefo() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Ceftriaxone 1g EQ\"}"), 7L);
        Store store = masterService.createStore(
                mapper.readTree("{\"code\":\"ENQ2\",\"description\":\"Enquiry Store 2\"}"), 7L);
        com.hims.item.domain.model.ItemBatch far = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"FAR-EQ\",\"expiryDate\":\"2028-12-31\"}"), 7L);
        com.hims.item.domain.model.ItemBatch near = masterService.createBatch(mapper.readTree(
                "{\"itemId\":" + item.getId() + ",\"batchCode\":\"NEAR-EQ\",\"expiryDate\":\"2027-01-31\"}"), 7L);
        stockService.postMovement(store.getId(), item.getId(), far.getId(), null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(50), "OPENING", null, null);
        stockService.postMovement(store.getId(), item.getId(), near.getId(), null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(50), "OPENING", null, null);

        var view = enquiryService.getBatches(item.getId(), store.getId());
        assertThat(view).extracting("batchCode").containsExactly("NEAR-EQ", "FAR-EQ");
    }

    @Test
    void itemEnquiry_stock_acrossStores_andFiltered() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Diclofenac EQ\"}"), 7L);
        Store s1 = masterService.createStore(
                mapper.readTree("{\"code\":\"EQS1\",\"description\":\"Enquiry Store 1\"}"), 7L);
        Store s2 = masterService.createStore(
                mapper.readTree("{\"code\":\"EQS2\",\"description\":\"Enquiry Store 2\"}"), 7L);
        stockService.postMovement(s1.getId(), item.getId(), null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(7), "OPENING", null, null);
        stockService.postMovement(s2.getId(), item.getId(), null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(3), "OPENING", null, null);

        // across stores
        var all = enquiryService.getStock(item.getId(), null);
        assertThat(all).extracting("storeId").containsExactly(s1.getId(), s2.getId());
        assertThat(all.stream().map(com.hims.item.domain.model.ItemStockBalance::getAvailableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10");

        // filtered by store
        var s1Only = enquiryService.getStock(item.getId(), s1.getId());
        assertThat(s1Only).hasSize(1);
        assertThat(s1Only.get(0).getStoreId()).isEqualTo(s1.getId());
        assertThat(s1Only.get(0).getAvailableQty()).isEqualByComparingTo("7");
    }

    @Test
    void itemEnquiry_suppliers_returnsItemSupplierJoin() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Linked Item EQ\"}"), 7L);
        com.hims.item.domain.model.Supplier sup1 = supplierService.create(
                mapper.readTree("{\"code\":\"SUPEQ1\",\"description\":\"Enquiry Supplier 1\"}"));
        com.hims.item.domain.model.Supplier sup2 = supplierService.create(
                mapper.readTree("{\"code\":\"SUPEQ2\",\"description\":\"Enquiry Supplier 2\"}"));
        // link both suppliers to the item in a committed tx
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(s -> {
            em.createNativeQuery(
                    "INSERT INTO ihms_item.item_supplier (id, status, item_id, supplier_id, synchronized) "
                            + "VALUES (nextval('ihms_item.item_supplier_seq'), TRUE, "
                            + item.getId() + ", " + sup1.getId() + ", FALSE)").executeUpdate();
            em.createNativeQuery(
                    "INSERT INTO ihms_item.item_supplier (id, status, item_id, supplier_id, synchronized) "
                            + "VALUES (nextval('ihms_item.item_supplier_seq'), TRUE, "
                            + item.getId() + ", " + sup2.getId() + ", FALSE)").executeUpdate();
        });

        var rows = enquiryService.getSuppliers(item.getId());
        assertThat(rows).extracting("supplierCode").containsExactly("SUPEQ1", "SUPEQ2");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.supplierId()).isNotNull();
            assertThat(r.itemSupplierId()).isNotNull();
        });
    }

    @Test
    void itemEnquiry_suppliers_emptyListWhenNoLinks() throws Exception {
        Item item = masterService.createItem(
                mapper.readTree("{\"itemName\":\"Unlinked Item EQ\"}"), 7L);
        assertThat(enquiryService.getSuppliers(item.getId())).isEmpty();
    }
}
