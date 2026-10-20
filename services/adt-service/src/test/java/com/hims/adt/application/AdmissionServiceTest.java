package com.hims.adt.application;

import com.hims.adt.application.exceptions.ActiveAdmissionExistsException;
import com.hims.adt.application.exceptions.AdtEntityNotFoundException;
import com.hims.adt.application.exceptions.BedUnavailableException;
import com.hims.adt.application.exceptions.InvalidAdtRequestException;
import com.hims.adt.domain.model.Admission;
import com.hims.adt.domain.model.AdmissionDetail;
import com.hims.adt.domain.model.Bed;
import com.hims.adt.domain.model.BedReservation;
import com.hims.adt.domain.model.BedStatusMaster;
import com.hims.adt.domain.model.BedTransferLog;
import com.hims.adt.domain.model.ClearanceChecklistItem;
import com.hims.adt.domain.model.ClearanceDepartment;
import com.hims.adt.domain.model.Ward;
import com.hims.adt.domain.repository.AdmissionDetailRepository;
import com.hims.adt.domain.repository.AdmissionRepository;
import com.hims.adt.domain.repository.AttendantPassRepository;
import com.hims.adt.domain.repository.BedRepository;
import com.hims.adt.domain.repository.BedReservationRepository;
import com.hims.adt.domain.repository.BedStatusMasterRepository;
import com.hims.adt.domain.repository.BedTransferLogRepository;
import com.hims.adt.domain.repository.ClearanceChecklistRepository;
import com.hims.adt.domain.repository.WardRepository;
import com.hims.adt.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — admission/transfer/discharge/clearance/abscond rules, no Spring context.
 * Covers the register's mandatory error paths (BERA022 / BERB013 / BERB005 /
 * BEXB001), the BR-IPD-001 ipdNo template, transfer CLEANING + log semantics,
 * the BR-DIS-001 clearance gate and the BR-IPD-005 abscond sweep.
 */
class AdmissionServiceTest {

    private AdmissionRepository admissionRepository;
    private AdmissionDetailRepository admissionDetailRepository;
    private BedRepository bedRepository;
    private BedReservationRepository reservationRepository;
    private BedTransferLogRepository transferLogRepository;
    private ClearanceChecklistRepository clearanceRepository;
    private BedStatusMasterRepository bedStatusMasterRepository;
    private WardRepository wardRepository;
    private AttendantPassRepository attendantPassRepository;
    private OutboxService outboxService;
    private EntityManager em;
    private AdmissionService service;
    private long nextSeq = 100;

    @BeforeEach
    void setUp() {
        admissionRepository = mock(AdmissionRepository.class);
        admissionDetailRepository = mock(AdmissionDetailRepository.class);
        bedRepository = mock(BedRepository.class);
        reservationRepository = mock(BedReservationRepository.class);
        transferLogRepository = mock(BedTransferLogRepository.class);
        clearanceRepository = mock(ClearanceChecklistRepository.class);
        bedStatusMasterRepository = mock(BedStatusMasterRepository.class);
        wardRepository = mock(WardRepository.class);
        attendantPassRepository = mock(AttendantPassRepository.class);
        outboxService = mock(OutboxService.class);
        em = mock(EntityManager.class);
        service = new AdmissionService(admissionRepository, admissionDetailRepository,
                bedRepository, reservationRepository, transferLogRepository,
                clearanceRepository, bedStatusMasterRepository, wardRepository,
                attendantPassRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        ReflectionTestUtils.setField(service, "ipdPrefix", "IHMS");

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> nextSeq++);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(admissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(admissionDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clearanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transferLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attendantPassRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Bed freeBed(long id, long wardId, short statusId) {
        Bed b = new Bed();
        b.setBedId(id);
        b.setBedNo("B-" + id);
        b.setWardId(wardId);
        b.setStatus(true);
        b.setOccupied(false);
        b.setBedStatusId(statusId);
        return b;
    }

    private BedStatusMaster master(short id, boolean isrelease) {
        BedStatusMaster m = new BedStatusMaster();
        m.setBedStatusId(id);
        m.setDescription("status " + id);
        m.setIsrelease(isrelease);
        return m;
    }

    private Ward ward(long id, Long unitId) {
        Ward w = new Ward();
        w.setWardId(id);
        w.setName("Ward-" + id);
        w.setUnitId(unitId);
        w.setStatus(true);
        return w;
    }

    private Admission activeAdmission(long id, long patientId, long bedId) {
        Admission a = new Admission();
        a.setAdmissionId(id);
        a.setPatientId(patientId);
        a.setBedId(bedId);
        a.setWardId(1L);
        a.setDoctorId(9L);
        a.setIpdNo("A/IHMS/26/" + String.format("%05d", id));
        a.setAdmittedAt(Timestamp.valueOf("2027-03-01 08:00:00"));
        a.setIsCancel(false);
        return a;
    }

    /** Happy admit stubs: patient 501 claims free bed 7 (status 6 master). */
    private Admission admitHappyPath() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        when(bedRepository.findById(7L))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 6)))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedStatusMasterRepository.findById((short) 6)).thenReturn(Optional.of(master((short) 6, true)));
        when(bedRepository.claimBed(7L)).thenReturn(1);
        when(wardRepository.findById(2L)).thenReturn(Optional.of(ward(2L, 1L)));
        return service.admit(501L, 900L, 11L, "ROUTINE", 1L, 7L, false, null, null, null, null);
    }

    // ------------------------------------------------------------- BR-IPD-001

    @Test
    void admit_missingDoctor_BERA022() {
        assertThatThrownBy(() -> service.admit(501L, null, null, "ROUTINE", 1L, null, false, null, null, null, null))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("BERA022");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void admit_duplicateActiveAdmission_BERB013() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(1L);

        assertThatThrownBy(() -> service.admit(501L, null, 11L, "ROUTINE", 1L, null, false, null, null, null, null))
                .isInstanceOf(ActiveAdmissionExistsException.class)
                .extracting(e -> ((ActiveAdmissionExistsException) e).getErrorCode())
                .isEqualTo("BERB013");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void admit_mlcWithoutNumber_rejected() {
        assertThatThrownBy(() -> service.admit(501L, null, 11L, "EMERGENCY", 1L, null, true, null, null, null, null))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_MLC_NUMBER_REQUIRED");
    }

    @Test
    void admit_happyPath_seedsDetailAndClearance_attendingDefaultsToAdmitting() {
        Admission saved = admitHappyPath();

        ArgumentCaptor<AdmissionDetail> detail = ArgumentCaptor.forClass(AdmissionDetail.class);
        verify(admissionDetailRepository).save(detail.capture());
        assertThat(detail.getValue().getAttendingDoctorId()).isEqualTo(11L);   // defaults to admitting doctor
        assertThat(detail.getValue().getIsMlc()).isFalse();

        ArgumentCaptor<ClearanceChecklistItem> rows = ArgumentCaptor.forClass(ClearanceChecklistItem.class);
        verify(clearanceRepository, org.mockito.Mockito.times(4)).save(rows.capture());
        assertThat(rows.getAllValues())
                .extracting(ClearanceChecklistItem::getDepartment)
                .containsExactlyInAnyOrder(ClearanceDepartment.values());
        assertThat(rows.getAllValues()).allMatch(ClearanceChecklistItem::pending);

        // E-03 PatientAdmitted — exact payload keys, partition key patientId
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq("PatientAdmitted"), eq("admission"), eq(saved.getAdmissionId()),
                eq("501"), payload.capture(), contains("patient-admitted:"));
        var node = readJson(payload.getValue());
        assertThat(node.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "admissionId", "patientId", "encounterId", "bedId", "wardId", "unitId",
                "admissionType", "ipdNo", "doctorId", "admittedAt");
    }

    // ------------------------------------------------------------- BR-IPD-001 ipdNo

    @Test
    void ipdNo_followsLegacyTemplate_atomicSequentialFromTheSequence() {
        Admission saved = admitHappyPath();
        int yy = Year.now().getValue() - 2000;
        // nextSeq started at 100: first draw is t_admission_seq=100
        assertThat(saved.getIpdNo())
                .isEqualTo("A/IHMS/%d/%05d".formatted(yy, saved.getAdmissionId()))
                .startsWith("A/IHMS/" + yy + "/")
                .hasSize("A/IHMS/26/00000".length());

        // sequential: a second admit draws the NEXT sequence value
        when(admissionRepository.countActiveByPatientId(502L)).thenReturn(0L);
        when(bedRepository.findById(8L))
                .thenReturn(Optional.of(freeBed(8L, 2L, (short) 1)))
                .thenReturn(Optional.of(freeBed(8L, 2L, (short) 5)));
        when(bedStatusMasterRepository.findById((short) 1)).thenReturn(Optional.of(master((short) 1, true)));
        when(bedRepository.claimBed(8L)).thenReturn(1);
        when(wardRepository.findById(2L)).thenReturn(Optional.of(ward(2L, 1L)));
        Admission second = service.admit(502L, null, 11L, "ROUTINE", 1L, 8L, false, null, null, null, null);

        assertThat(second.getIpdNo()).isEqualTo("A/IHMS/%d/%05d".formatted(yy, second.getAdmissionId()));
        // %05d overflow past 99999 pads naturally to 6 digits (catalog boundary case)
        assertThat(service.buildIpdNo(100_000)).endsWith("/100000");
    }

    // ------------------------------------------------------------- BR-IPD-003

    @Test
    void admit_nonReleaseBed_BERB005() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        when(bedRepository.findById(5L)).thenReturn(Optional.of(freeBed(5L, 1L, (short) 5)));
        when(bedStatusMasterRepository.findById((short) 5)).thenReturn(Optional.of(master((short) 5, false)));

        assertThatThrownBy(() -> service.admit(501L, null, 11L, "ROUTINE", 1L, 5L, false, null, null, null, null))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("BERB005");
        verify(bedRepository, never()).claimBed(5L);
    }

    // ------------------------------------------------------------- BR-IPD-002

    @Test
    void admit_autoAllocation_prefersUnitWards_thenLowestBedId_andSkipsForeignReservations() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        Bed reservedForOther = freeBed(3L, 2L, (short) 1);   // unit ward but RESERVED for someone else
        Bed unitBedLow = freeBed(4L, 2L, (short) 1);
        when(bedRepository.findFreeBeds()).thenReturn(new java.util.ArrayList<>(List.of(reservedForOther, unitBedLow)));
        when(reservationRepository.findFirstByBedIdAndResStatus(3L, BedReservation.ResStatus.ACTIVE))
                .thenReturn(Optional.of(activeReservation(3L, 999L)));      // other patient
        when(reservationRepository.findFirstByBedIdAndResStatus(4L, BedReservation.ResStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(wardRepository.findByUnitIdAndStatusTrue(1L)).thenReturn(List.of(ward(2L, 1L)));
        when(bedRepository.claimBed(4L)).thenReturn(1);
        when(bedRepository.findById(4L)).thenReturn(Optional.of(freeBed(4L, 2L, (short) 5)));
        when(wardRepository.findById(2L)).thenReturn(Optional.of(ward(2L, 1L)));

        Admission saved = service.admit(501L, null, 11L, "ROUTINE", 1L, null, false, null, null, null, null);

        assertThat(saved.getBedId()).isEqualTo(4L);          // reserved-for-other bed skipped
        verify(bedRepository, never()).claimBed(3L);
    }

    @Test
    void admit_reservedForSamePatient_isAllowedAndConsumed() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        when(bedRepository.findById(7L))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 1)))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedStatusMasterRepository.findById((short) 1)).thenReturn(Optional.of(master((short) 1, true)));
        when(bedRepository.claimBed(7L)).thenReturn(1);
        when(wardRepository.findById(2L)).thenReturn(Optional.of(ward(2L, 1L)));

        service.admit(501L, null, 11L, "ROUTINE", 1L, 7L, false, null, null, null, null);

        verify(reservationRepository).consume(eq(7L), eq(501L), any(Timestamp.class));   // CONSUMED
    }

    @Test
    void admit_autoAllocation_casLostOnEveryCandidate_BEXB001_409() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        when(bedRepository.findFreeBeds()).thenReturn(new java.util.ArrayList<>(List.of(freeBed(4L, 2L, (short) 1))));
        when(reservationRepository.findFirstByBedIdAndResStatus(4L, BedReservation.ResStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(wardRepository.findByUnitIdAndStatusTrue(1L)).thenReturn(List.of());
        when(bedRepository.claimBed(4L)).thenReturn(0);

        assertThatThrownBy(() -> service.admit(501L, null, 11L, "ROUTINE", 1L, null, false, null, null, null, null))
                .isInstanceOf(BedUnavailableException.class)
                .extracting(e -> ((BedUnavailableException) e).getErrorCode())
                .isEqualTo("BEXB001");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    private BedReservation activeReservation(long bedId, long patientId) {
        BedReservation r = new BedReservation();
        r.setReservationId(50L);
        r.setBedId(bedId);
        r.setPatientId(patientId);
        r.setReservedAt(Timestamp.from(java.time.Instant.now()));
        r.setExpiresAt(Timestamp.from(java.time.Instant.now().plusSeconds(3600)));
        r.setResStatus(BedReservation.ResStatus.ACTIVE);
        return r;
    }

    // ------------------------------------------------------------- reservations

    @Test
    void reserve_duplicateActiveOnSameBed_rejected409() {
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 1)));
        when(reservationRepository.findFirstByBedIdAndResStatus(7L, BedReservation.ResStatus.ACTIVE))
                .thenReturn(Optional.of(activeReservation(7L, 501L)));

        assertThatThrownBy(() -> service.reserve(7L, 502L,
                Timestamp.from(java.time.Instant.now().plusSeconds(600))))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_BED_RESERVED");
    }

    @Test
    void reserve_pastExpiry_rejected400() {
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 1)));
        assertThatThrownBy(() -> service.reserve(7L, 501L, Timestamp.valueOf("2020-01-01 00:00:00")))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_REQ_INVALID");
    }

    // ------------------------------------------------------------- transfer

    @Test
    void transfer_oldBedToCleaning_logRecorded_eventsEmitted() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));

        Bed target = freeBed(12L, 3L, (short) 1);
        when(bedRepository.findById(12L)).thenReturn(Optional.of(target));
        when(bedStatusMasterRepository.findById((short) 1)).thenReturn(Optional.of(master((short) 1, true)));
        when(bedRepository.claimBed(12L)).thenReturn(1);
        when(bedRepository.releaseBed(7L, (short) 2)).thenReturn(1);       // CLEANING

        Admission transferred = service.transfer(1L, 12L, "upgrade");

        assertThat(transferred.getBedId()).isEqualTo(12L);
        assertThat(transferred.getWardId()).isEqualTo(3L);
        verify(bedRepository).releaseBed(7L, (short) 2);                   // vacated -> CLEANING

        ArgumentCaptor<BedTransferLog> logRow = ArgumentCaptor.forClass(BedTransferLog.class);
        verify(transferLogRepository).save(logRow.capture());
        assertThat(logRow.getValue().getFromBedId()).isEqualTo(7L);
        assertThat(logRow.getValue().getToBedId()).isEqualTo(12L);
        assertThat(logRow.getValue().getFromWardId()).isEqualTo(1L);
        assertThat(logRow.getValue().getToWardId()).isEqualTo(3L);

        // BedTransferred keyed by patientId + TWO BedStatusChanged keyed by bedId
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(outboxService, org.mockito.Mockito.times(3))
                .append(type.capture(), any(), any(), key.capture(), any(), any());
        assertThat(type.getAllValues())
                .containsExactly("BedStatusChanged", "BedStatusChanged", "BedTransferred");
        assertThat(key.getAllValues()).containsExactly("7", "12", "501");

        ArgumentCaptor<String> transferredPayload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq("BedTransferred"), any(), eq(1L), eq("501"),
                transferredPayload.capture(), anyString());
        var node = readJson(transferredPayload.getValue());
        assertThat(node.get("fromBedId").asText()).isEqualTo("7");
        assertThat(node.get("toBedId").asText()).isEqualTo("12");
    }

    @Test
    void transfer_casLossOnTarget_BEXB001_oldBedUntouched() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        when(bedRepository.findById(12L)).thenReturn(Optional.of(freeBed(12L, 3L, (short) 1)));
        when(bedStatusMasterRepository.findById((short) 1)).thenReturn(Optional.of(master((short) 1, true)));
        when(bedRepository.claimBed(12L)).thenReturn(0);

        assertThatThrownBy(() -> service.transfer(1L, 12L, null)).isInstanceOf(BedUnavailableException.class);
        verify(bedRepository, never()).releaseBed(anyLong(), anyShort());
    }

    @Test
    void cleaningComplete_onlyFromCleaningState() {
        // non-cleaning bed rejected outright
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 1)));
        assertThatThrownBy(() -> service.markCleaningComplete(7L))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_BED_NOT_CLEANING");

        // CLEANING -> FREE: first lookup sees CLEANING, post-release re-read sees FREE
        when(bedRepository.releaseBed(7L, (short) 1)).thenReturn(1);
        when(bedRepository.findById(7L))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 2)))
                .thenReturn(Optional.of(freeBed(7L, 2L, (short) 1)));
        service.markCleaningComplete(7L);
        verify(bedRepository).releaseBed(7L, (short) 1);
        verify(outboxService).append(eq("BedStatusChanged"), eq("bed"), eq(7L),
                eq("7"), anyString(), contains("2->1"));
    }

    // ------------------------------------------------------------- discharge

    @Test
    void discharge_blockedWhileAnyClearancePending() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        when(clearanceRepository.countByAdmissionIdAndClrStatus(1L, ClearanceChecklistItem.ClrStatus.PENDING))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.discharge(1L, "NORMAL", false, null, null))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_CLEARANCE_PENDING");
        verify(bedRepository, never()).releaseBed(anyLong(), anyShort());
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void discharge_allClear_releasesBedStatus6_emitsExactEvents() {
        Admission active = activeAdmission(1L, 501L, 7L);
        active.setEncounterId(900L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        when(clearanceRepository.countByAdmissionIdAndClrStatus(1L, ClearanceChecklistItem.ClrStatus.PENDING))
                .thenReturn(0L);                                            // all four cleared
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedRepository.releaseBed(7L, (short) 6)).thenReturn(1);

        Admission discharged = service.discharge(1L, "NORMAL", false, null, null);

        assertThat(discharged.getDischargeDate()).isNotNull();
        assertThat(discharged.getDeathDate()).isNull();
        verify(bedRepository).releaseBed(7L, (short) 6);                    // BR-DIS-003 literal

        ArgumentCaptor<String> dischargePayload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq("PatientDischarged"), eq("discharge"), anyLong(),
                eq("501"), dischargePayload.capture(), contains("patient-discharged:"));
        var dnode = readJson(dischargePayload.getValue());
        assertThat(dnode.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "dischargeId", "patientId", "admissionId", "encounterId",
                "dischargeType", "isExpired", "deathDate", "date", "balanceDue");
        assertThat(dnode.get("balanceDue").isNull()).isTrue();
        assertThat(dnode.get("encounterId").asText()).isEqualTo("900");

        verify(outboxService).append(eq("BedStatusChanged"), eq("bed"), eq(7L), eq("7"),
                contains("\"newStatus\":\"6\""), contains("5->6"));
    }

    @Test
    void discharge_deathRecordsDeathDate_BR_DIS_002() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        when(clearanceRepository.countByAdmissionIdAndClrStatus(1L, ClearanceChecklistItem.ClrStatus.PENDING))
                .thenReturn(0L);
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedRepository.releaseBed(anyLong(), anyShort())).thenReturn(1);

        Timestamp death = Timestamp.valueOf("2027-03-02 06:30:00");
        Admission discharged = service.discharge(1L, "EXPIRED", true, death, null);

        assertThat(discharged.getIsExpired()).isTrue();
        assertThat(discharged.getDeathDate()).isEqualTo(death);
        assertThat(discharged.isDeath()).isTrue();
        verify(outboxService).append(eq("PatientDischarged"), any(), anyLong(), eq("501"),
                contains("\"isExpired\":true"), anyString());
    }

    @Test
    void discharge_abscondType_bypassesClearanceGate() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        // NOTE: pending count NOT stubbed — would blow up if consulted
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedRepository.releaseBed(anyLong(), anyShort())).thenReturn(1);

        Admission discharged = service.discharge(1L, AdmissionService.DISCHARGE_ABSCOND, false, null, null);
        assertThat(discharged.getDischargeType()).isEqualTo("ABSCOND");
        verify(outboxService).append(eq("PatientDischarged"), any(), anyLong(), eq("501"),
                anyString(), anyString());
    }

    @Test
    void abscond_manual_stampsAbscondTime_thenDischarges() {
        Admission active = activeAdmission(1L, 501L, 7L);
        active.setBedId(null);                       // interim unbedded admission
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));

        Admission result = service.abscond(1L, "left against advice");
        assertThat(result.getAbscondDatetime()).isNotNull();
        assertThat(result.getDischargeType()).isEqualTo("ABSCOND");
        assertThat(result.isActive()).isFalse();
    }

    // ------------------------------------------------------------- clearance

    @Test
    void clearDepartment_unknownDepartment_rejected() {
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(activeAdmission(1L, 501L, 7L)));
        assertThatThrownBy(() -> service.clearDepartment(1L, "HOUSEKEEPING", null, null))
                .isInstanceOf(InvalidAdtRequestException.class)
                .extracting(e -> ((InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_UNKNOWN_DEPARTMENT");
    }

    @Test
    void clearDepartment_pendingRow_clearsOnce_idempotently() {
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(activeAdmission(1L, 501L, 7L)));
        ClearanceChecklistItem row = new ClearanceChecklistItem();
        row.setClearanceId(77L);
        row.setAdmissionId(1L);
        row.setDepartment(ClearanceDepartment.PHARMACY);
        row.setClrStatus(ClearanceChecklistItem.ClrStatus.PENDING);
        when(clearanceRepository.findByAdmissionIdAndDepartment(1L, ClearanceDepartment.PHARMACY))
                .thenReturn(Optional.of(row));

        ClearanceChecklistItem cleared = service.clearDepartment(1L, "PHARMACY", 42L, "no returns");
        assertThat(cleared.pending()).isFalse();
        assertThat(cleared.getClearedBy()).isEqualTo(42L);

        // second call keeps the CLEARED state (idempotent re-clearance)
        ClearanceChecklistItem again = service.clearDepartment(1L, "PHARMACY", 43L, null);
        assertThat(again.getClearedBy()).isEqualTo(42L);   // first stamp wins
    }

    // ------------------------------------------------------------- abscond sweep

    @Test
    void sweepAbsconds_sweepsOnlyUnbeddedStaleInterimAdmissions() {
        Admission stale = activeAdmission(1L, 501L, 0L);
        stale.setBedId(null);
        Admission fresh = activeAdmission(2L, 502L, 0L);
        fresh.setBedId(null);
        fresh.setAdmittedAt(Timestamp.from(java.time.Instant.now()));     // inside window
        when(admissionRepository.findInterimUnbeddedBefore(any()))
                .thenReturn(List.of(stale));                              // repo does the cutoff filter
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(stale));   // discharge() reload

        java.util.List<Admission> swept = service.sweepAbsconds(
                Timestamp.from(java.time.Instant.now().minus(java.time.Duration.ofDays(3))));

        assertThat(swept).hasSize(1);
        assertThat(stale.getAbscondDatetime()).isNotNull();
        assertThat(stale.getDischargeType()).isEqualTo(AdmissionService.DISCHARGE_AUTO_ABSCONDED);
        verify(outboxService, org.mockito.Mockito.atLeastOnce()).append(
                eq("PatientDischarged"), any(), anyLong(), eq("501"), anyString(), anyString());
    }

    // ------------------------------------------------------------- M03 interim / attendant / census / tid / death

    @Test
    void admitInterim_noBedClaim_createsInterimAdmission() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(0L);
        Admission interim = service.admitInterim(501L, 900L, 11L, "INTERIM", 1L, false, null, null, null, null, "TID123");
        assertThat(interim.getIsInterim()).isTrue();
        assertThat(interim.getBedId()).isNull();
        assertThat(interim.getTid()).isEqualTo("TID123");
        assertThat(interim.getIpdNo()).startsWith("A/IHMS/");
        verify(bedRepository, never()).claimBed(anyLong());
        verify(outboxService).append(eq("PatientAdmitted"), eq("admission"), eq(interim.getAdmissionId()),
                eq("501"), anyString(), contains("patient-admitted-interim:"));
    }

    @Test
    void admitInterim_duplicateActive_BERB013() {
        when(admissionRepository.countActiveByPatientId(501L)).thenReturn(1L);
        assertThatThrownBy(() -> service.admitInterim(501L, null, 11L, null, 1L, false, null, null, null, null, null))
                .isInstanceOf(ActiveAdmissionExistsException.class);
    }

    @Test
    void issueAttendantPass_createsIssuedPass_withAttNo() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        var p = service.issueAttendantPass(1L, "Ram Kumar", 100L);
        assertThat(p.getPassNo()).startsWith("ATT/");
        assertThat(p.getPassStatus()).isEqualTo("ISSUED");
        assertThat(p.getAttendeeName()).isEqualTo("Ram Kumar");
        assertThat(p.getPrintCount()).isEqualTo(1L);
    }

    @Test
    void issueAttendantPass_inactiveAdmission_rejected() {
        Admission discharged = activeAdmission(1L, 501L, 7L);
        discharged.setDischargeDate(Timestamp.valueOf("2027-03-02 10:00:00"));
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(discharged));
        assertThatThrownBy(() -> service.issueAttendantPass(1L, "X", null))
                .isInstanceOf(InvalidAdtRequestException.class);
    }

    @Test
    void returnAttendantPass_issuedToReturned() {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(10L); p.setAdmissionId(1L); p.setPatientId(501L);
        p.setPassNo("ATT/26/00001"); p.setPassStatus("ISSUED"); p.setPrintCount(1L);
        when(attendantPassRepository.findById(10L)).thenReturn(Optional.of(p));
        var returned = service.returnAttendantPass(10L);
        assertThat(returned.getPassStatus()).isEqualTo("RETURNED");
        assertThat(returned.getReturnDate()).isNotNull();
    }

    @Test
    void returnAttendantPass_alreadyReturned_rejected() {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(10L); p.setPassStatus("RETURNED");
        when(attendantPassRepository.findById(10L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.returnAttendantPass(10L))
                .isInstanceOf(InvalidAdtRequestException.class);
    }

    @Test
    void reprint_bumpsPrintCount() {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(10L); p.setPassStatus("ISSUED"); p.setPrintCount(1L);
        when(attendantPassRepository.findById(10L)).thenReturn(Optional.of(p));
        var r = service.reprintAttendantPass(10L);
        assertThat(r.getPrintCount()).isEqualTo(2L);
    }

    @Test
    void census_perWardCounts() {
        Ward w = ward(2L, 1L);
        when(wardRepository.findAll()).thenReturn(List.of(w));
        when(bedRepository.findByWardIdAndStatusTrue(2L)).thenReturn(List.of(
                freeBed(10L, 2L, (short) 1), // free
                freeBed(11L, 2L, (short) 5)  // occupied (status 5 but occupied true)
        ));
        // second bed should be occupied true for occupied count
        Bed occupied = freeBed(11L, 2L, (short) 5);
        occupied.setOccupied(true);
        when(bedRepository.findByWardIdAndStatusTrue(2L)).thenReturn(List.of(freeBed(10L, 2L, (short) 1), occupied));
        when(admissionRepository.countByWardIdAndDischargeDateIsNullAndIsCancelFalse(2L)).thenReturn(1L);
        when(admissionRepository.findInterimUnbeddedBefore(any())).thenReturn(List.of());
        var census = service.census(null);
        assertThat(census).containsKey("wards");
        var wards = (List<Map<String, Object>>) census.get("wards");
        assertThat(wards).hasSize(1);
        assertThat(wards.get(0).get("wardId")).isEqualTo(2L);
    }

    @Test
    void tidList_returnsOnlyWithTid() {
        Admission withTid = activeAdmission(1L, 501L, 7L);
        withTid.setTid("TID123");
        when(admissionRepository.findWithTid()).thenReturn(List.of(withTid));
        assertThat(service.tidList()).hasSize(1);
        assertThat(service.tidList().get(0).getTid()).isEqualTo("TID123");
    }

    @Test
    void deathList_andDischargeWithDeathCause() {
        Admission active = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(active));
        when(clearanceRepository.countByAdmissionIdAndClrStatus(1L, ClearanceChecklistItem.ClrStatus.PENDING)).thenReturn(0L);
        when(bedRepository.findById(7L)).thenReturn(Optional.of(freeBed(7L, 2L, (short) 5)));
        when(bedRepository.releaseBed(anyLong(), anyShort())).thenReturn(1);
        Admission discharged = service.discharge(1L, "EXPIRED", true,
                Timestamp.valueOf("2027-03-02 06:30:00"), null, "Cardiac arrest", "NATURAL");
        assertThat(discharged.getIsExpired()).isTrue();
        assertThat(discharged.getDeathCause()).isEqualTo("Cardiac arrest");
        assertThat(discharged.getDeathType()).isEqualTo("NATURAL");
        when(admissionRepository.findDeaths()).thenReturn(List.of(discharged));
        assertThat(service.deathList()).hasSize(1);
    }

    @Test
    void setTid_updatesAdmission() {
        Admission a = activeAdmission(1L, 501L, 7L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(a));
        Admission updated = service.setTid(1L, "NEWTID");
        assertThat(updated.getTid()).isEqualTo("NEWTID");
    }

    // ------------------------------------------------------------- misc

    @Test
    void get_unknownAdmission_404() {
        when(admissionRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(AdtEntityNotFoundException.class);
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
