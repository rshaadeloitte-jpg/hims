package com.hims.equipment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentTransfer;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentTransferRepository;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — Equipment asset-register business rules (registration defaults, per-unit
 *  accession uniqueness, null-safe patch update incl. base-class {@code status},
 *  guarded atomic transfer, gated search params). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EquipmentServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private EquipmentTransferRepository transferRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepository, transferRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence stub: every nextval(...) yields 1
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(EquipmentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        // default: accession free
        when(equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(any(), anyString()))
                .thenReturn(Optional.empty());
        when(equipmentRepository.findFirstByAccessionNoIgnoreCaseOrderByIdDesc(anyString()))
                .thenReturn(Optional.empty());
    }

    // ---------------------------------------------------------------- create

    @Test
    void register_validData_assignsSeqIdDefaultsAndAuditStamps() throws Exception {
        Equipment saved = service.register(mapper.readTree("""
                {"itemId":501,"accessionNo":"ACC-100","serialNo":"SN-9","model":"V300",
                 "make":"Drager","departmentId":10,"cost":125000.50}
                """), 7L);

        assertThat(saved.getId()).isEqualTo(1L); // from mocked equipment_seq nextval
        assertThat(saved.getItemId()).isEqualTo(501L);
        assertThat(saved.getWorkingStatus()).isEqualTo(EquipmentWorkingStatus.WORKING); // default
        assertThat(saved.getStatus()).isTrue();
        assertThat(saved.getUnitId()).isEqualTo(7L);
        assertThat(saved.getCreatedUnitId()).isEqualTo(7L);
        assertThat(saved.getUpdatedUnitId()).isEqualTo(7L);
        assertThat(saved.getAddedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);
        assertThat(saved.getAddedDatetime()).isNotNull();
        assertThat(saved.getUpdatedBy()).isNull();
        assertThat(saved.getUpdatedDatetime()).isNull();
        assertThat(saved.getSynchronizedd()).isFalse();
        assertThat(saved.getCost()).isEqualByComparingTo(new BigDecimal("125000.50"));
        verify(em).createNativeQuery(anyString()); // sequence drawn server-side
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void register_explicitWorkingStatusAndInactiveStatus_respected() throws Exception {
        Equipment saved = service.register(mapper.readTree(
                "{\"itemId\":1,\"workingStatus\":3,\"status\":false}"), 7L);
        assertThat(saved.getWorkingStatus()).isEqualTo(EquipmentWorkingStatus.UNDER_REPAIR);
        assertThat(saved.getStatus()).isFalse();
    }

    @Test
    void register_missingItemId_throws() {
        assertThatThrownBy(() -> service.register(mapper.readTree("{\"accessionNo\":\"X\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("itemId");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void register_invalidWorkingStatus_throws() {
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"itemId\":1,\"workingStatus\":9}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid workingStatus");
    }

    @Test
    void register_nullPayload_throws() {
        assertThatThrownBy(() -> service.register(null, 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
    }

    @Test
    void register_duplicateAccessionInUnit_throws_andDoesNotSave() {
        when(equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(eq(7L), eq("ACC-DUP")))
                .thenReturn(Optional.of(equipment(2L, 7L, "ACC-DUP")));

        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"itemId\":1,\"accessionNo\":\"ACC-DUP\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("already exists in this unit");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void register_duplicateAccessionWithoutUnit_fallsBackToGlobalCheck() {
        when(equipmentRepository.findFirstByAccessionNoIgnoreCaseOrderByIdDesc("ACC-G"))
                .thenReturn(Optional.of(equipment(3L, null, "ACC-G")));

        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"itemId\":1,\"accessionNo\":\"ACC-G\"}"), null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("already exists");
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_appliesPatchIncludingBaseClassStatus_andStampsAudit() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        existing.setStatus(true);
        existing.setModel("Old Model");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Equipment out = service.update(1L, mapper.readTree("""
                {"model":"Ventilator Pro","cost":99000.00,"status":false}
                """));

        assertThat(out.getModel()).isEqualTo("Ventilator Pro");           // Equipment field
        assertThat(out.getCost()).isEqualByComparingTo(new BigDecimal("99000.00"));
        assertThat(out.getStatus()).isFalse();                            // EquipmentBase field (regression:
        //   applyUpdate must walk into the superclass or deactivation silently no-ops)
        assertThat(out.getUpdatedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);
        assertThat(out.getUpdatedDatetime()).isNotNull();
        assertThat(out.getAddedBy()).isNull();                            // untouched
        verify(equipmentRepository).save(existing);
    }

    @Test
    void update_neverTakesServerOwnedColumnsFromPayload() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Equipment out = service.update(1L, mapper.readTree("""
                {"id":999,"unitId":555,"createdUnitId":555,"updatedUnitId":555,
                 "addedBy":42,"updatedBy":42,"synchronizedd":true,"model":"M2"}
                """));

        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getUnitId()).isEqualTo(7L);
        assertThat(out.getCreatedUnitId()).isNull();
        assertThat(out.getUpdatedUnitId()).isNull();
        assertThat(out.getAddedBy()).isNull();
        assertThat(out.getSynchronizedd()).isFalse();
        assertThat(out.getModel()).isEqualTo("M2");                       // ordinary fields still patch
    }

    @Test
    void update_sameAccessionDifferentCase_skipsRevalidation() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Equipment out = service.update(1L, mapper.readTree("{\"accessionNo\":\"acc-001\"}"));
        assertThat(out.getAccessionNo()).isEqualTo("acc-001");
        // self-match must not be treated as a duplicate
        verify(equipmentRepository, never())
                .findByUnitIdAndAccessionNoIgnoreCase(any(), anyString());
    }

    @Test
    void update_changedAccessionAlreadyUsedByOtherAsset_throws() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(eq(7L), eq("ACC-TAKEN")))
                .thenReturn(Optional.of(equipment(9L, 7L, "ACC-TAKEN")));

        assertThatThrownBy(() -> service.update(1L,
                mapper.readTree("{\"accessionNo\":\"ACC-TAKEN\"}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("already exists in this unit");
    }

    @Test
    void update_unknownEquipment_throws() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, mapper.readTree("{\"model\":\"x\"}")))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    // ---------------------------------------------------------------- search

    @Test
    void search_byUnitWorkingStatusAndFreeText_passesGatedParams() {
        service.search(7L, EquipmentWorkingStatus.NOT_WORKING, "  vent  ");
        // sentinel binding: real values + true gates when filter present, trimmed text
        verify(equipmentRepository).search(7L, true, 2L, true, "vent", true);
    }

    @Test
    void search_withoutFilters_passesSentinelsWithFalseGates() {
        service.search(null, null, null);
        verify(equipmentRepository).search(0L, false, 0L, false, "", false);
    }

    @Test
    void search_invalidWorkingStatus_throws() {
        assertThatThrownBy(() -> service.search(7L, 9L, null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid workingStatus");
    }

    // -------------------------------------------------------------- accession

    @Test
    void findByAccession_blank_throws() {
        assertThatThrownBy(() -> service.findByAccession(" ", 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("accessionNo");
    }

    @Test
    void findByAccession_usesUnitScopeWhenGiven() {
        Equipment e = equipment(4L, 7L, "ACC-007");
        when(equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(7L, "ACC-007"))
                .thenReturn(Optional.of(e));
        assertThat(service.findByAccession("ACC-007", 7L)).isSameAs(e);
    }

    @Test
    void findByAccession_withoutUnit_fallsBackToNewestMatch() {
        Equipment e = equipment(4L, null, "ACC-008");
        when(equipmentRepository.findFirstByAccessionNoIgnoreCaseOrderByIdDesc("ACC-008"))
                .thenReturn(Optional.of(e));
        assertThat(service.findByAccession("ACC-008", null)).isSameAs(e);
    }

    @Test
    void findByAccession_unknown_throwsNotFound() {
        assertThatThrownBy(() -> service.findByAccession("NOPE", 7L))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void findByItem_delegatesToRepository() {
        Equipment e = equipment(5L, 7L, "ACC-009");
        when(equipmentRepository.findByItemIdAndStatusTrueOrderByIdDesc(501L)).thenReturn(List.of(e));
        assertThat(service.findByItem(501L)).containsExactly(e);
    }

    // --------------------------------------------------------------- transfer

    @Test
    void transfer_happyPath_writesLogRowAndMovesDepartmentAtomically() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        existing.setDepartmentId(10L);
        existing.setLocation("WARD-1");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        EquipmentTransfer t = service.transfer(1L, mapper.readTree("""
                {"toDepartmentId":22,"reason":"ICU reallocation",
                 "transferDate":"2025-01-15T00:00:00Z","toLocation":"ICU-2","toStoreId":33}
                """));

        assertThat(t.getId()).isEqualTo(1L);                              // equipment_transfer_seq
        assertThat(t.getEquipmentId()).isEqualTo(1L);
        assertThat(t.getFromDepartmentId()).isEqualTo(10L);
        assertThat(t.getToDepartmentId()).isEqualTo(22L);
        assertThat(t.getTransferDate()).isEqualTo(Timestamp.from(Instant.parse("2025-01-15T00:00:00Z")));
        assertThat(t.getReason()).isEqualTo("ICU reallocation");
        assertThat(t.getStatus()).isTrue();
        assertThat(t.getUnitId()).isEqualTo(7L);
        assertThat(t.getAddedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);

        assertThat(existing.getDepartmentId()).isEqualTo(22L);            // move committed together
        assertThat(existing.getLocation()).isEqualTo("ICU-2");
        assertThat(existing.getStoreId()).isEqualTo(33L);
        assertThat(existing.getUpdatedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);
        verify(transferRepository, times(1)).save(any(EquipmentTransfer.class));
        verify(equipmentRepository, times(1)).save(existing);
    }

    @Test
    void transfer_defaultsTransferDateAndLeavesUntouchedFieldsAlone() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        existing.setDepartmentId(10L);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        EquipmentTransfer t = service.transfer(1L, mapper.readTree("{\"toDepartmentId\":30}"));
        assertThat(t.getTransferDate()).isNotNull();                      // now()
        assertThat(existing.getLocation()).isNull();                      // no toLocation in payload
        assertThat(existing.getStoreId()).isNull();
        assertThat(existing.getDepartmentId()).isEqualTo(30L);
    }

    @Test
    void transfer_inactiveAsset_throws() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        existing.setStatus(false);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.transfer(1L, mapper.readTree("{\"toDepartmentId\":22}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("inactive");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_condemnedAsset_throws() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        existing.setWorkingStatus(EquipmentWorkingStatus.CONDEMNED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.transfer(1L, mapper.readTree("{\"toDepartmentId\":22}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("condemned");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_missingToDepartment_throws() throws Exception {
        Equipment existing = equipment(1L, 7L, "ACC-001");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.transfer(1L, mapper.readTree("{\"reason\":\"no target\"}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("toDepartmentId");
    }

    // ------------------------------------------------------------------ reads

    @Test
    void getEquipment_unknown_throwsNotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getEquipment(99L))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void transferHistory_unknownAsset_throwsBeforeQuery() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transferHistory(99L))
                .isInstanceOf(EquipmentNotFoundException.class);
        verify(transferRepository, never()).findByEquipmentIdOrderByIdAsc(any());
    }

    @Test
    void transferHistory_returnsChronologicalLog() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(1L, 7L, "ACC-001")));
        EquipmentTransfer t1 = new EquipmentTransfer();
        t1.setId(11L);
        t1.setEquipmentId(1L);
        EquipmentTransfer t2 = new EquipmentTransfer();
        t2.setId(12L);
        t2.setEquipmentId(1L);
        when(transferRepository.findByEquipmentIdOrderByIdAsc(1L)).thenReturn(List.of(t1, t2));

        List<EquipmentTransfer> history = service.transferHistory(1L);
        assertThat(history).containsExactly(t1, t2);
    }

    // ------------------------------------------------------------- helpers

    private Equipment equipment(long id, Long unitId, String accessionNo) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setItemId(501L);
        e.setAccessionNo(accessionNo);
        e.setUnitId(unitId);
        e.setStatus(true);
        return e;
    }
}
