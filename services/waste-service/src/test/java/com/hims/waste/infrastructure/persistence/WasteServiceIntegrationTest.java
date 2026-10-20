package com.hims.waste.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.waste.WasteServiceApplication;
import com.hims.waste.application.BioMedicalWasteService;
import com.hims.waste.application.WasteBagMasterService;
import com.hims.waste.domain.model.BioMedicalWaste;
import com.hims.waste.domain.model.BioMedicalWasteDetails;
import com.hims.waste.domain.model.WasteBagMaster;
import com.hims.waste.domain.model.WasteStatus;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — MIG-023 waste service on real PG16: V1 schema applies (ihms_waste),
 * bag master via sequence, and the full request lifecycle (raise -> approve ->
 * handover -> receive -> process) with bag expansion, barcode allocation, and
 * the 5-state machine guards.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = WasteServiceApplication.class)
@DirtiesContext
@Transactional
class WasteServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WasteBagMasterService bagMasterService;

    @Autowired
    private BioMedicalWasteService wasteService;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void resetSchema() {
        // setval is non-transactional in PG: survives the test-tx rollback, so each
        // test deterministically starts ids at fixed bases.
        em.createNativeQuery("SELECT setval('ihms_waste.\"waste_bag_master_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_waste.\"bio_medical_waste_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_waste.\"bio_medical_waste_details_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_waste.\"bio_medical_waste_req_no_seq\"', 4000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_waste.bio_medical_waste_details, ihms_waste.bio_medical_waste,
                             ihms_waste.waste_bag_master, ihms_waste.barcode_counter CASCADE""")
                    .executeUpdate();
        });
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_waste' AND table_name IN
                          ('waste_bag_master','bio_medical_waste','bio_medical_waste_details','barcode_counter')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("barcode_counter", "bio_medical_waste",
                "bio_medical_waste_details", "waste_bag_master");
    }

    @Test
    void bagMaster_createAndList() throws Exception {
        WasteBagMaster b = bagMasterService.createBag(
                mapper.readTree("{\"code\":\"YEL\",\"name\":\"Yellow Bag\",\"description\":\"Infectious\"}"), 7L);
        assertThat(b.getId()).isNotNull();
        assertThat(b.getCode()).isEqualTo("YEL");
        assertThat(b.getUnitId()).isEqualTo(7L);
        assertThat(bagMasterService.listBags(7L)).hasSize(1);
    }

    @Test
    void fullLifecycle_raiseApproveHandoverReceiveProcess() throws Exception {
        WasteBagMaster bag = bagMasterService.createBag(
                mapper.readTree("{\"code\":\"YEL\",\"name\":\"Yellow Bag\"}"), 7L);

        BioMedicalWaste r = wasteService.raiseRequest(mapper.readTree("""
                {"request":{"storeId":10,"toStoreId":20},
                 "details":[{"bagId":%d,"quantity":2,"remark":"infectious"}]}
                """.formatted(bag.getId())), 7L, "RISL");

        assertThat(r.getRequisitionNo()).matches("RISL/BW/\\d{2}/\\d{5}");
        assertThat(r.getRequestStatus()).isEqualTo(WasteStatus.PENDING);

        List<BioMedicalWasteDetails> bags = wasteService.getDetails(r.getId());
        assertThat(bags).hasSize(2);
        assertThat(bags).allSatisfy(d -> {
            assertThat(d.getQuantity()).isEqualTo(1L);
            assertThat(d.getBarcodeNo()).matches("\\d{13}");
            assertThat(d.getBagStatus()).isEqualTo(WasteStatus.PENDING);
        });
        // barcodes unique
        assertThat(bags.get(0).getBarcodeNo()).isNotEqualTo(bags.get(1).getBarcodeNo());

        String barcode = bags.get(0).getBarcodeNo();
        assertThat(wasteService.validateBarcode(r.getId(), barcode)).isTrue();
        assertThat(wasteService.validateBarcode(r.getId(), "9999999999999")).isFalse();

        // approve (em.clear(): REQUIRES_NEW writes commit outside the test tx, so the
        // test persistence context must be cleared to see the committed bag statuses)
        BioMedicalWaste approved = wasteService.approve(r.getId());
        assertThat(approved.getRequestStatus()).isEqualTo(WasteStatus.ACCEPTED);
        em.clear();
        assertThat(wasteService.getDetails(r.getId()))
                .allSatisfy(d -> assertThat(d.getBagStatus()).isEqualTo(WasteStatus.ACCEPTED));

        // handover
        BioMedicalWaste handed = wasteService.handover(r.getId(), "EcoFirm");
        assertThat(handed.getRequestStatus()).isEqualTo(WasteStatus.HANDOVERED);
        assertThat(handed.getHandedOverTo()).isEqualTo("EcoFirm");
        em.clear();

        // receive with per-bag weight
        em.clear();
        BioMedicalWasteDetails firstBag = wasteService.getDetails(r.getId()).get(0);
        BioMedicalWaste received = wasteService.receive(r.getId(), "StoreKeeper",
                mapper.readTree("[{\"id\":%d,\"weight\":2.5}]".formatted(firstBag.getId())));
        assertThat(received.getRequestStatus()).isEqualTo(WasteStatus.RECEIVED);
        assertThat(received.getReceivedBy()).isEqualTo("StoreKeeper");

        // process
        BioMedicalWaste completed = wasteService.process(r.getId(), "EcoFirm", "John", 9876543210L);
        assertThat(completed.getRequestStatus()).isEqualTo(WasteStatus.COMPLETED);
        em.clear();
        assertThat(wasteService.getDetails(r.getId()))
                .allSatisfy(d -> {
                    assertThat(d.getBagStatus()).isEqualTo(WasteStatus.COMPLETED);
                    assertThat(d.getFirmName()).isEqualTo("EcoFirm");
                });

        // counts
        Map<String, Long> counts = wasteService.getCounts();
        assertThat(counts.get("TOTAL")).isEqualTo(1L);
        assertThat(counts.get("COMPLETED")).isEqualTo(1L);
    }

    @Test
    void barcodeCounter_incrementsAcrossRequests() throws Exception {
        WasteBagMaster bag = bagMasterService.createBag(
                mapper.readTree("{\"code\":\"RED\",\"name\":\"Red Bag\"}"), 7L);

        BioMedicalWaste r1 = wasteService.raiseRequest(mapper.readTree("""
                {"request":{"storeId":1},"details":[{"bagId":%d,"quantity":1}]}
                """.formatted(bag.getId())), 7L, null);
        BioMedicalWaste r2 = wasteService.raiseRequest(mapper.readTree("""
                {"request":{"storeId":1},"details":[{"bagId":%d,"quantity":1}]}
                """.formatted(bag.getId())), 7L, null);

        String b1 = wasteService.getDetails(r1.getId()).get(0).getBarcodeNo();
        String b2 = wasteService.getDetails(r2.getId()).get(0).getBarcodeNo();
        assertThat(b1).isNotEqualTo(b2);
        // same yyyyMM prefix, counter incremented
        assertThat(b1.substring(0, 6)).isEqualTo(b2.substring(0, 6));
        long c1 = Long.parseLong(b1.substring(6));
        long c2 = Long.parseLong(b2.substring(6));
        assertThat(c2).isEqualTo(c1 + 1);
    }

    @Test
    void softDelete_setsStatusFalseAndRequestStatusZero() throws Exception {
        BioMedicalWaste r = wasteService.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), 7L, null);
        BioMedicalWaste deleted = wasteService.deleteRequest(r.getId());
        assertThat(deleted.getStatus()).isFalse();
        assertThat(deleted.getRequestStatus()).isEqualTo(0L);
        // soft-deleted excluded from list
        assertThat(wasteService.listRequests(7L, null, null)).isEmpty();
    }

    @Test
    void search_numericMatchesBarcode_nonNumericMatchesRequisition() throws Exception {
        WasteBagMaster bag = bagMasterService.createBag(
                mapper.readTree("{\"code\":\"BLU\",\"name\":\"Blue Bag\"}"), 7L);
        BioMedicalWaste r = wasteService.raiseRequest(mapper.readTree("""
                {"request":{"storeId":1},"details":[{"bagId":%d,"quantity":1}]}
                """.formatted(bag.getId())), 7L, "RISL");
        String barcode = wasteService.getDetails(r.getId()).get(0).getBarcodeNo();

        // numeric search matches barcode
        assertThat(wasteService.listRequests(7L, null, barcode)).hasSize(1);
        // non-numeric search matches requisitionNo
        assertThat(wasteService.listRequests(7L, null, "RISL/BW")).hasSize(1);
        // non-matching
        assertThat(wasteService.listRequests(7L, null, "ZZZZ")).isEmpty();
    }
}
