package com.hims.waste.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.waste.application.exceptions.InvalidWasteRequestException;
import com.hims.waste.application.exceptions.WasteNotFoundException;
import com.hims.waste.domain.model.BioMedicalWaste;
import com.hims.waste.domain.model.BioMedicalWasteDetails;
import com.hims.waste.domain.model.WasteStatus;
import com.hims.waste.domain.repository.BioMedicalWasteDetailsRepository;
import com.hims.waste.domain.repository.BioMedicalWasteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** L1 — BMW request lifecycle business rules (5-state machine, bag expansion, barcode, guards). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BioMedicalWasteServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private BioMedicalWasteRepository requestRepository;
    @Mock private BioMedicalWasteDetailsRepository detailsRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BioMedicalWasteService service;

    @BeforeEach
    void setUp() {
        service = new BioMedicalWasteService(requestRepository, detailsRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence + barcode counter stubs
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(requestRepository.save(any(BioMedicalWaste.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detailsRepository.save(any(BioMedicalWasteDetails.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void raiseRequest_stampsRequisitionNoStatusAndExpandsBags() throws Exception {
        BioMedicalWaste r = service.raiseRequest(mapper.readTree("""
                {"request":{"storeId":10,"toStoreId":20},
                 "details":[{"bagId":1,"quantity":3,"remark":"yellow"},{"bagId":2,"quantity":0}]}
                """), 7L, "RISL");
        assertThat(r.getRequisitionNo()).matches("RISL/BW/\\d{2}/\\d{5}");
        assertThat(r.getRequestStatus()).isEqualTo(WasteStatus.PENDING);
        assertThat(r.getRequestDate()).isNotNull();
        assertThat(r.getUnitId()).isEqualTo(7L);
        assertThat(r.getAddedBy()).isEqualTo(BioMedicalWasteService.AUDIT_ACTOR_ID);
        // quantity=3 expands to 3 bag rows; quantity=0 skipped
        org.mockito.Mockito.verify(detailsRepository, org.mockito.Mockito.times(3))
                .save(any(BioMedicalWasteDetails.class));
    }

    @Test
    void raiseRequest_defaultUnitPrefix() throws Exception {
        BioMedicalWaste r = service.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), null, null);
        assertThat(r.getRequisitionNo()).startsWith("IHMS/BW/");
    }

    @Test
    void raiseRequest_missingRequestNode_throws() {
        assertThatThrownBy(() -> service.raiseRequest(mapper.readTree("{\"details\":[]}"), 1L, null))
                .isInstanceOf(InvalidWasteRequestException.class);
    }

    @Test
    void approve_pendingToAccepted_stampsBags() {
        BioMedicalWaste r = request(WasteStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        BioMedicalWasteDetails bag = bag(1L, WasteStatus.PENDING);
        when(detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(1L)).thenReturn(List.of(bag));

        BioMedicalWaste out = service.approve(1L);
        assertThat(out.getRequestStatus()).isEqualTo(WasteStatus.ACCEPTED);
        assertThat(bag.getBagStatus()).isEqualTo(WasteStatus.ACCEPTED);
    }

    @Test
    void approve_wrongStatus_throws() {
        BioMedicalWaste r = request(WasteStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(InvalidWasteRequestException.class)
                .hasMessageContaining("approve");
    }

    @Test
    void handover_requiresHandedOverTo() {
        BioMedicalWaste r = request(WasteStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.handover(1L, " "))
                .isInstanceOf(InvalidWasteRequestException.class);
    }

    @Test
    void handover_acceptedToHandovered_stampsBags() {
        BioMedicalWaste r = request(WasteStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        BioMedicalWasteDetails bag = bag(1L, WasteStatus.ACCEPTED);
        when(detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(1L)).thenReturn(List.of(bag));

        BioMedicalWaste out = service.handover(1L, "FirmA");
        assertThat(out.getRequestStatus()).isEqualTo(WasteStatus.HANDOVERED);
        assertThat(out.getHandedOverTo()).isEqualTo("FirmA");
        assertThat(bag.getHandedOverTo()).isEqualTo("FirmA");
    }

    @Test
    void receive_handoveredToReceived() {
        BioMedicalWaste r = request(WasteStatus.HANDOVERED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        when(detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(1L)).thenReturn(List.of());

        BioMedicalWaste out = service.receive(1L, "StoreKeeper", null);
        assertThat(out.getRequestStatus()).isEqualTo(WasteStatus.RECEIVED);
        assertThat(out.getReceivedBy()).isEqualTo("StoreKeeper");
    }

    @Test
    void process_receivedToCompleted_stampsFirm() {
        BioMedicalWaste r = request(WasteStatus.RECEIVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        BioMedicalWasteDetails bag = bag(1L, WasteStatus.RECEIVED);
        when(detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(1L)).thenReturn(List.of(bag));

        BioMedicalWaste out = service.process(1L, "EcoFirm", "John", 999L);
        assertThat(out.getRequestStatus()).isEqualTo(WasteStatus.COMPLETED);
        assertThat(bag.getFirmName()).isEqualTo("EcoFirm");
        assertThat(bag.getContactPerson()).isEqualTo("John");
        assertThat(bag.getContactNo()).isEqualTo(999L);
    }

    @Test
    void deleteRequest_softDelete() {
        BioMedicalWaste r = request(WasteStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        BioMedicalWaste out = service.deleteRequest(1L);
        assertThat(out.getStatus()).isFalse();
        assertThat(out.getRequestStatus()).isEqualTo(0L);
    }

    @Test
    void getRequest_unknown_throws() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRequest(99L))
                .isInstanceOf(WasteNotFoundException.class);
    }

    @Test
    void updateBagStatus_advancesAndRedrivesMin() throws Exception {
        BioMedicalWaste r = request(WasteStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        BioMedicalWasteDetails bag = bag(5L, WasteStatus.PENDING);
        bag.setBioMedicalWasteId(1L);
        when(detailsRepository.findById(5L)).thenReturn(Optional.of(bag));
        when(detailsRepository.minBagStatus(1L)).thenReturn(Optional.of(WasteStatus.ACCEPTED));

        BioMedicalWaste out = service.updateBagStatus(1L, mapper.readTree(
                "[{\"id\":5,\"bagStatus\":2,\"handedOverTo\":\"FirmB\"}]"));
        assertThat(bag.getBagStatus()).isEqualTo(WasteStatus.ACCEPTED);
        assertThat(bag.getHandedOverTo()).isEqualTo("FirmB");
        assertThat(out.getRequestStatus()).isEqualTo(WasteStatus.ACCEPTED);
        assertThat(out.getHandedOverTo()).contains("FirmB");
    }

    @Test
    void validateBarcode_blank_throws() {
        assertThatThrownBy(() -> service.validateBarcode(1L, " "))
                .isInstanceOf(InvalidWasteRequestException.class);
    }

    // ------------------------------------------------------------- helpers

    private BioMedicalWaste request(long status) {
        BioMedicalWaste r = new BioMedicalWaste();
        r.setId(1L);
        r.setRequisitionNo("IHMS/BW/26/00001");
        r.setRequestStatus(status);
        r.setStatus(true);
        return r;
    }

    private BioMedicalWasteDetails bag(long id, long status) {
        BioMedicalWasteDetails d = new BioMedicalWasteDetails();
        d.setId(id);
        d.setBioMedicalWasteId(1L);
        d.setBagStatus(status);
        d.setQuantity(1L);
        return d;
    }
}
