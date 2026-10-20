package com.hims.bcma.application;

import com.hims.bcma.domain.model.*;
import com.hims.bcma.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BcmaModuleServiceTest {

    @Mock private MedicationScanRepository scanRepo;
    @Mock private IVPEventRepository ivpRepo;
    @Mock private BCMAExceptionRepository excRepo;
    @Mock private MedicationStockRepository stockRepo;

    @InjectMocks private BcmaModuleService service;

    @BeforeEach
    void setUp() { }

    @Test
    void scanMedicationReturnsPatientDrugMatch() {
        when(scanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PatientDrugMatch result = service.scanMedication(101L, "D123", "Paracetamol", 1L, 10L);
        assertNotNull(result);
        assertEquals(101L, result.patientId());
        assertTrue(result.isMatch());
        assertEquals("MATCH", result.verificationStatus());
        verify(scanRepo).save(any(MedicationScan.class));
    }

    @Test
    void scanMedicationWithNullDrugIenStillSaves() {
        when(scanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PatientDrugMatch result = service.scanMedication(102L, null, null, 2L, null);
        assertNotNull(result);
        assertEquals("MATCH", result.verificationStatus());
    }

    @Test
    void recordIVPCreatesActiveEventWhenEndTimeNull() {
        when(ivpRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        Timestamp start = Timestamp.from(Instant.now());
        IVPEvent ev = service.recordIVP(202L, "Morphine", "5mg", "10ml/min", start, null, 5L, 3L);
        assertNotNull(ev);
        assertEquals("Morphine", ev.getDrugName());
        assertEquals("ACTIVE", ev.getEventStatus());
        assertEquals(202L, ev.getPatientId());
    }

    @Test
    void recordIVPCreatesCompletedWhenEndTimePresent() {
        when(ivpRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        Timestamp start = Timestamp.from(Instant.now());
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(300));
        IVPEvent ev = service.recordIVP(203L, "Fentanyl", "2mcg", "2ml/min", start, end, 7L, 4L);
        assertEquals("COMPLETED", ev.getEventStatus());
    }

    @Test
    void logExceptionRefusedCreatesException() {
        when(excRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        BCMAException ex = service.logException(301L, "D99", BCMAException.ExceptionType.REFUSED, "Patient refused", 9L, Timestamp.from(Instant.now()), 6L, 3L);
        assertEquals("REFUSED", ex.getExceptionType());
        assertEquals("Patient refused", ex.getReason());
        assertEquals(9L, ex.getOverrideNurse());
    }

    @Test
    void logExceptionMissedCreatesException() {
        when(excRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        BCMAException ex = service.logException(302L, null, BCMAException.ExceptionType.MISSED, "Nurse missed dose", null, Timestamp.from(Instant.now()), null, 1L);
        assertEquals("MISSED", ex.getExceptionType());
    }

    @Test
    void logExceptionDelayedCreatesException() {
        when(excRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        BCMAException ex = service.logException(303L, "D88", BCMAException.ExceptionType.DELAYED, "Delayed administration", 10L, Timestamp.from(Instant.now()), 2L, 4L);
        assertEquals("DELAYED", ex.getExceptionType());
    }

    @Test
    void getStockByMedicationWithDrugAndWardReturnsStock() {
        MedicationStock s = new MedicationStock(); s.setStockId(1L); s.setDrugIen("D55"); s.setWardId(7L); s.setAvailableQty(java.math.BigDecimal.valueOf(12));
        when(stockRepo.findByDrugIenAndWardId("D55", 7L)).thenReturn(Optional.of(s));
        List<MedicationStock> res = service.getStockByMedication("D55", 7L);
        assertEquals(1, res.size());
        assertEquals(0, BigDecimal.valueOf(12).compareTo(res.get(0).getAvailableQty()));
    }

    @Test
    void getStockByMedicationWithOnlyDrugReturnsList() {
        when(stockRepo.findByDrugIen("D55")).thenReturn(List.of(new MedicationStock()));
        assertEquals(1, service.getStockByMedication("D55", null).size());
    }

    @Test
    void getStockByMedicationWithOnlyWardReturnsList() {
        when(stockRepo.findByWardId(7L)).thenReturn(List.of(new MedicationStock(), new MedicationStock()));
        assertEquals(2, service.getStockByMedication(null, 7L).size());
    }

    @Test
    void getStockByMedicationWithNoFiltersReturnsAll() {
        when(stockRepo.findAll()).thenReturn(List.of(new MedicationStock()));
        assertEquals(1, service.getStockByMedication(null, null).size());
    }

    @Test
    void getScanResultReturnsMatch() {
        MedicationScan s = new MedicationScan(); s.setScanId(999L); s.setPatientId(1L); s.setDrugIen("D1"); s.setVerificationStatus("MATCH"); s.setVerified(true);
        when(scanRepo.findByScanId(999L)).thenReturn(Optional.of(s));
        PatientDrugMatch m = service.getScanResult(999L);
        assertNotNull(m);
        assertTrue(m.isMatch());
    }

    @Test
    void getScanResultReturnsNullWhenNotFound() {
        when(scanRepo.findByScanId(888L)).thenReturn(Optional.empty());
        assertNull(service.getScanResult(888L));
    }
}
