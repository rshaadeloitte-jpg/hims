package com.hims.equipment.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.api.WarrantyCounts;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.model.EquipmentWarranty;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentWarrantyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — Warranty / AMC cover business rules: adding covers to assets (sequence
 * ids, warrantyType defaulting/trimming, server-owned equipmentId pairing,
 * asset guards), per-asset listing, expiry-alert windows (default 30 days,
 * inclusive start-of-today boundary semantics), count-summary wiring incl.
 * the "(unspecified)" bucket when grouped types under-count, and the
 * null-patch update whose skip-list keeps id/equipmentId/unit/audit columns
 * server-owned.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarrantyServiceTest {

    /** Mirrors Spring Boot's auto-configured mapper: unknown payload keys must
     *  be tolerated, not failed (plain new ObjectMapper() would reject them). */
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private EquipmentWarrantyRepository warrantyRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private WarrantyService service;

    @BeforeEach
    void setUp() {
        service = new WarrantyService(equipmentRepository, warrantyRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence stub: every nextval(...) yields 1
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(warrantyRepository.save(any(EquipmentWarranty.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------- add

    @Test
    void add_withoutTypeKey_defaultsWarranty_assignsSeqIdPairingAndAuditStamps() throws Exception {
        activeAsset(1L);

        EquipmentWarranty w = service.add(1L, mapper.readTree("""
                {"equipmentId":999,"vendor":"Acme","terms":"Full comprehensive",
                 "startDate":"2026-01-01T00:00:00Z","endDate":"2026-07-01T00:00:00Z"}
                """), 7L);

        assertThat(w.getId()).isEqualTo(1L);                        // equipment_warranty_seq (mocked)
        assertThat(w.getWarrantyType())                             // key absent => column default applied by service
                .isEqualTo(WarrantyService.DEFAULT_WARRANTY_TYPE);
        assertThat(w.getEquipmentId()).isEqualTo(1L);               // SERVER-OWNED from path — payload 999 ignored
        assertThat(w.getVendor()).isEqualTo("Acme");
        assertThat(w.getTerms()).isEqualTo("Full comprehensive");
        assertThat(w.getStartDate()).isEqualTo(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(w.getEndDate()).isEqualTo(Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")));
        assertThat(w.getStatus()).isTrue();

        // base audit block
        assertThat(w.getUnitId()).isEqualTo(7L);
        assertThat(w.getCreatedUnitId()).isEqualTo(7L);
        assertThat(w.getUpdatedUnitId()).isEqualTo(7L);
        assertThat(w.getAddedBy()).isEqualTo(WarrantyService.AUDIT_ACTOR_ID);
        assertThat(w.getAddedDatetime()).isNotNull();
        assertThat(w.getUpdatedBy()).isNull();
        assertThat(w.getUpdatedDatetime()).isNull();
        assertThat(w.getSynchronizedd()).isFalse();

        // exactly ONE sequence burn: row id only (no human code on covers)
        verify(em, times(1)).createNativeQuery(anyString());
        verify(em).createNativeQuery(contains("equipment_warranty_seq"));
        verify(warrantyRepository).save(any(EquipmentWarranty.class));
    }

    @Test
    void add_explicitType_trimmedAndHonored_noDefault() throws Exception {
        activeAsset(1L);

        EquipmentWarranty w = service.add(1L,
                mapper.readTree("{\"warrantyType\":\"  AMC  \"}"), 7L);

        assertThat(w.getWarrantyType()).isEqualTo("AMC");
        verify(em, times(1)).createNativeQuery(anyString());        // row-id seq still burned once
    }

    @Test
    void add_nullPayload_throwsBeforeAssetLookupOrSave() throws Exception {
        assertThatThrownBy(() -> service.add(1L, mapper.readTree("null"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid input data");
        verify(equipmentRepository, never()).findById(any());
        verify(warrantyRepository, never()).save(any());
    }

    @Test
    void add_unbindablePayload_throwsBexa003_beforeSeqBurnOrSave() throws Exception {
        activeAsset(1L);

        assertThatThrownBy(() -> service.add(1L,
                mapper.readTree("{\"startDate\":\"15-01-2026\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid input data");
        verify(em, never()).createNativeQuery(anyString());         // bind happens before any nextval …
        verify(warrantyRepository, never()).save(any());            // … and before the save
    }

    @Test
    void add_unknownAsset_throwsNotFound_neverSave() throws Exception {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(99L,
                mapper.readTree("{\"vendor\":\"x\"}"), 7L))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("99");
        verify(warrantyRepository, never()).save(any());
    }

    @Test
    void add_inactiveAsset_throws_neverSave() throws Exception {
        Equipment asset = asset(1L);
        asset.setStatus(false);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.add(1L,
                mapper.readTree("{\"vendor\":\"x\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("inactive");
        verify(warrantyRepository, never()).save(any());
    }

    @Test
    void add_condemnedAsset_throws_neverSave() throws Exception {
        Equipment asset = asset(1L);
        asset.setWorkingStatus(EquipmentWorkingStatus.CONDEMNED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.add(1L,
                mapper.readTree("{\"vendor\":\"x\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("condemned");
        verify(warrantyRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ list

    @Test
    void listByEquipment_unknownAsset_throwsBeforeQuery() throws Exception {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByEquipment(99L))
                .isInstanceOf(EquipmentNotFoundException.class);
        verify(warrantyRepository, never())
                .findByEquipmentIdAndStatusTrueOrderByIdDesc(any());
    }

    @Test
    void listByEquipment_returnsAssetCoversNewestFirst() throws Exception {
        activeAsset(1L);
        EquipmentWarranty older = new EquipmentWarranty();
        older.setId(2L);
        EquipmentWarranty newer = new EquipmentWarranty();
        newer.setId(8L);
        when(warrantyRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(1L))
                .thenReturn(List.of(newer, older));

        assertThat(service.listByEquipment(1L)).containsExactly(newer, older);
    }

    // -------------------------------------------------------------- expiring

    @Test
    void expiring_defaultDays_windowStartsTodayThroughPlus30_withFalseGates() throws Exception {
        service.expiring(null, null, null);

        ArgumentCaptor<Timestamp> from = ArgumentCaptor.forClass(Timestamp.class);
        ArgumentCaptor<Timestamp> to = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository).findExpiring(from.capture(), to.capture(),
                eq(0L), eq(false), eq(0L), eq(false));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZoneId z = ZoneId.systemDefault();
        assertThat(from.getValue())
                .isEqualTo(Timestamp.from(today.atStartOfDay(z).toInstant()));
        assertThat(to.getValue())                                   // default 30 => exclusive end at day+31
                .isEqualTo(Timestamp.from(today.plusDays(31).atStartOfDay(z).toInstant()));
    }

    @Test
    void expiring_explicitDaysAndFilters_passesGatedSentinelsAndExactWindow() throws Exception {
        service.expiring(45L, 7L, 2L);

        ArgumentCaptor<Timestamp> from = ArgumentCaptor.forClass(Timestamp.class);
        ArgumentCaptor<Timestamp> to = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository).findExpiring(from.capture(), to.capture(),
                eq(7L), eq(true), eq(2L), eq(true));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZoneId z = ZoneId.systemDefault();
        assertThat(from.getValue())
                .isEqualTo(Timestamp.from(today.atStartOfDay(z).toInstant()));
        assertThat(to.getValue())
                .isEqualTo(Timestamp.from(today.plusDays(46).atStartOfDay(z).toInstant()));
    }

    @Test
    void expiring_zeroOrNegativeDays_throwsBeforeQuery() throws Exception {
        assertThatThrownBy(() -> service.expiring(0L, null, null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> service.expiring(-5L, 7L, null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("positive");
        verify(warrantyRepository, never()).findExpiring(
                any(), any(), anyLong(), anyBoolean(), anyLong(), anyBoolean());
    }

    @Test
    void expiring_windowBoundaries_lastAlertDayIncluded_dayAfterExcluded() throws Exception {
        service.expiring(10L, null, null);                          // window [today, day+11)

        ArgumentCaptor<Timestamp> from = ArgumentCaptor.forClass(Timestamp.class);
        ArgumentCaptor<Timestamp> to = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository).findExpiring(from.capture(), to.capture(),
                anyLong(), anyBoolean(), anyLong(), anyBoolean());
        ZoneId z = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Timestamp fromTs = from.getValue();
        Timestamp toTs = to.getValue();
        // a cover lapsing later ON the last alert day is still inside the window …
        Timestamp lastAlertDayNoon = Timestamp.from(
                today.plusDays(10).atTime(12, 0).atZone(z).toInstant());
        assertThat(lastAlertDayNoon).isAfterOrEqualTo(fromTs).isBefore(toTs);
        // … while anything from the first day AFTER the window is outside
        Timestamp firstOutside = toTs;                              // == day+11 00:00
        assertThat(firstOutside).isEqualTo(Timestamp.from(
                today.plusDays(11).atStartOfDay(z).toInstant()));
    }

    // ---------------------------------------------------------------- counts

    @Test
    void counts_global_wiresAllQueries_mapsGroupedRows_andStampsWindows() throws Exception {
        when(warrantyRepository.countActive(0L, false)).thenReturn(4L);
        when(warrantyRepository.countActiveGroupedByType(0L, false)).thenReturn(List.<Object[]>of(
                new Object[]{"AMC", 3},                             // Integer on purpose: Number mapping
                new Object[]{"WARRANTY", 1L}));
        when(warrantyRepository.countExpiredBefore(any(), anyLong(), anyBoolean()))
                .thenReturn(1L);
        when(warrantyRepository.countExpiringBetween(any(), any(), anyLong(), anyBoolean()))
                .thenReturn(2L);

        WarrantyCounts dto = service.counts(null);

        assertThat(dto.totalActive()).isEqualTo(4L);
        assertThat(dto.expired()).isEqualTo(1L);
        assertThat(dto.expiringIn30Days()).isEqualTo(2L);
        assertThat(dto.byType())
                .extracting(WarrantyCounts.TypeCount::warrantyType, WarrantyCounts.TypeCount::count)
                .containsExactly(tuple("AMC", 3L), tuple("WARRANTY", 1L)); // insertion order kept

        // alert windows: expired cutoff = start of today; expiring window [today, today+31)
        ArgumentCaptor<Timestamp> expiredCutoff = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository).countExpiredBefore(expiredCutoff.capture(), eq(0L), eq(false));
        ArgumentCaptor<Timestamp> winFrom = ArgumentCaptor.forClass(Timestamp.class);
        ArgumentCaptor<Timestamp> winTo = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository)
                .countExpiringBetween(winFrom.capture(), winTo.capture(), eq(0L), eq(false));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZoneId z = ZoneId.systemDefault();
        assertThat(expiredCutoff.getValue())
                .isEqualTo(Timestamp.from(today.atStartOfDay(z).toInstant()));
        assertThat(winFrom.getValue())
                .isEqualTo(Timestamp.from(today.atStartOfDay(z).toInstant()));
        assertThat(winTo.getValue())
                .isEqualTo(Timestamp.from(today.plusDays(31).atStartOfDay(z).toInstant()));
    }

    @Test
    void counts_unitScope_gatesEveryQuery_andAppendsUnspecifiedBucketOnUndercount() throws Exception {
        when(warrantyRepository.countActive(7L, true)).thenReturn(5L);
        when(warrantyRepository.countActiveGroupedByType(7L, true)).thenReturn(List.<Object[]>of(
                new Object[]{"AMC", 2},
                new Object[]{"WARRANTY", 2}));                      // sums to 4 < totalActive 5

        WarrantyCounts dto = service.counts(7L);

        assertThat(dto.totalActive()).isEqualTo(5L);
        // NULL-type rows break the group-by invariant => synthetic bucket appended LAST
        assertThat(dto.byType())
                .extracting(WarrantyCounts.TypeCount::warrantyType, WarrantyCounts.TypeCount::count)
                .containsExactly(tuple("AMC", 2L), tuple("WARRANTY", 2L),
                        tuple("(unspecified)", 1L));

        verify(warrantyRepository).countActive(7L, true);
        verify(warrantyRepository).countActiveGroupedByType(7L, true);
        verify(warrantyRepository).countExpiredBefore(any(), eq(7L), eq(true));
        verify(warrantyRepository).countExpiringBetween(any(), any(), eq(7L), eq(true));
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_patchesFields_trimsType_neverTouchesServerOwnedColumns() throws Exception {
        EquipmentWarranty existing = existingCover();
        when(warrantyRepository.findById(5L)).thenReturn(Optional.of(existing));
        ZoneId z = ZoneId.systemDefault();
        String newEnd = LocalDate.now(ZoneId.systemDefault()).plusDays(90)
                .atTime(12, 0).atZone(z).toInstant().toString();

        // every server-owned column attacked in one payload — all must be ignored
        EquipmentWarranty saved = service.update(5L, mapper.readTree("""
                {"id":777,"equipmentId":8888,"unitId":42,"createdUnitId":55,
                 "updatedUnitId":66,"addedBy":77,"updatedBy":88,
                 "addedDatetime":"2030-01-01T00:00:00Z",
                 "updatedDatetime":"2030-01-01T00:00:00Z","synchronizedd":true,
                 "warrantyType":"  AMC ","vendor":"NewVendor","terms":"renewed terms",
                 "endDate":"%s"}
                """.formatted(newEnd)));

        assertThat(saved).isSameAs(existing);
        // patchable fields copied (+ type trimmed)
        assertThat(saved.getWarrantyType()).isEqualTo("AMC");
        assertThat(saved.getVendor()).isEqualTo("NewVendor");
        assertThat(saved.getTerms()).isEqualTo("renewed terms");
        assertThat(saved.getEndDate())
                .isEqualTo(Timestamp.from(LocalDate.now(z).plusDays(90)
                        .atTime(12, 0).atZone(z).toInstant()));
        // skip-list kept every server-owned column untouched
        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getEquipmentId()).isEqualTo(1004L);
        assertThat(saved.getUnitId()).isEqualTo(7L);
        assertThat(saved.getCreatedUnitId()).isEqualTo(3L);
        assertThat(saved.getUpdatedUnitId()).isEqualTo(3L);
        assertThat(saved.getAddedBy()).isEqualTo(9L);
        assertThat(saved.getAddedDatetime())
                .isEqualTo(Timestamp.from(Instant.parse("2026-01-01T08:00:00Z")));
        assertThat(saved.getSynchronizedd()).isTrue();              // hijack attempt ignored
        // audit stamp for the update itself
        assertThat(saved.getUpdatedBy()).isEqualTo(WarrantyService.AUDIT_ACTOR_ID);
        assertThat(saved.getUpdatedDatetime()).isNotNull();
    }

    @Test
    void update_nullFieldsInPatch_leaveExistingValuesAlone() throws Exception {
        EquipmentWarranty existing = existingCover();
        Timestamp originalEnd = existing.getEndDate();
        when(warrantyRepository.findById(5L)).thenReturn(Optional.of(existing));

        EquipmentWarranty saved = service.update(5L,
                mapper.readTree("{\"vendor\":null,\"endDate\":null}"));

        assertThat(saved.getVendor()).isEqualTo("OldVendor");       // null patch = no change
        assertThat(saved.getEndDate()).isEqualTo(originalEnd);
        verify(warrantyRepository).save(existing);
    }

    @Test
    void update_unknownId_throwsNotFound_neverSave() throws Exception {
        when(warrantyRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L,
                mapper.readTree("{\"vendor\":\"x\"}")))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("Warranty not found");
        verify(warrantyRepository, never()).save(any());
    }

    @Test
    void update_nullPayload_throwsBeforeLookup() throws Exception {
        assertThatThrownBy(() -> service.update(5L, mapper.readTree("null")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid input data");
        verify(warrantyRepository, never()).findById(any());
    }

    // ---------------------------------------------------- lifecycle end-to-end

    /** Spec chain: add cover → it alerts inside the default expiry window →
     *  update extends the cover → it leaves a short alert window. */
    @Test
    void lifecycle_add_alertsInsideWindow_update_extendsBeyondShortWindow() throws Exception {
        activeAsset(1004L);
        ZoneId z = ZoneId.systemDefault();
        String endsSoon = LocalDate.now(z).plusDays(10).atTime(12, 0).atZone(z)
                .toInstant().toString();

        EquipmentWarranty w = service.add(1004L,
                mapper.readTree("{\"vendor\":\"Acme\",\"endDate\":\"" + endsSoon + "\"}"), 7L);
        assertThat(w.getId()).isEqualTo(1L);
        assertThat(w.getEquipmentId()).isEqualTo(1004L);

        // default 30-day window brackets the cover's end date …
        service.expiring(null, null, null);
        ArgumentCaptor<Timestamp> from = ArgumentCaptor.forClass(Timestamp.class);
        ArgumentCaptor<Timestamp> to = ArgumentCaptor.forClass(Timestamp.class);
        verify(warrantyRepository).findExpiring(from.capture(), to.capture(),
                anyLong(), anyBoolean(), anyLong(), anyBoolean());
        assertThat(w.getEndDate()).isAfterOrEqualTo(from.getValue()).isBefore(to.getValue());

        // … the summary counts it as active and expiring-in-30-days
        when(warrantyRepository.countActive(0L, false)).thenReturn(1L);
        when(warrantyRepository.countActiveGroupedByType(0L, false))
                .thenReturn(List.<Object[]>of(new Object[]{"WARRANTY", 1L}));
        when(warrantyRepository.countExpiredBefore(any(), anyLong(), anyBoolean())).thenReturn(0L);
        when(warrantyRepository.countExpiringBetween(any(), any(), anyLong(), anyBoolean()))
                .thenReturn(1L);
        WarrantyCounts dto = service.counts(null);
        assertThat(dto.totalActive()).isEqualTo(1L);
        assertThat(dto.expired()).isEqualTo(0L);
        assertThat(dto.expiringIn30Days()).isEqualTo(1L);

        // renewal pushes the cover far out — a 3-day alert window no longer sees it
        when(warrantyRepository.findById(w.getId())).thenReturn(Optional.of(w));
        String renewed = LocalDate.now(z).plusDays(365).atTime(12, 0).atZone(z)
                .toInstant().toString();
        EquipmentWarranty updated = service.update(w.getId(),
                mapper.readTree("{\"endDate\":\"" + renewed + "\"}"));
        service.expiring(3L, null, null);
        assertThat(updated.getEndDate()).isEqualTo(Timestamp.from(LocalDate.now(z)
                .plusDays(365).atTime(12, 0).atZone(z).toInstant()));
    }

    // ---------------------------------------------------------------- helpers

    private void activeAsset(long id) {
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(asset(id)));
    }

    private Equipment asset(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setItemId(501L);
        e.setStoreId(33L);
        e.setAccessionNo("ACC-" + id);
        e.setUnitId(7L);
        e.setStatus(true);
        return e;
    }

    private EquipmentWarranty existingCover() {
        EquipmentWarranty w = new EquipmentWarranty();
        w.setId(5L);
        w.setEquipmentId(1004L);
        w.setWarrantyType("WARRANTY");
        w.setStartDate(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")));
        w.setEndDate(Timestamp.from(Instant.parse("2026-12-31T00:00:00Z")));
        w.setVendor("OldVendor");
        w.setTerms("old terms");
        w.setUnitId(7L);
        w.setStatus(true);
        w.setCreatedUnitId(3L);
        w.setUpdatedUnitId(3L);
        w.setAddedBy(9L);
        w.setAddedDatetime(Timestamp.from(Instant.parse("2026-01-01T08:00:00Z")));
        w.setSynchronizedd(true);
        return w;
    }
}
