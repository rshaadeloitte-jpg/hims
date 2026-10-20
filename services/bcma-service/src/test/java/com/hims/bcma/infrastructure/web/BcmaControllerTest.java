package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.BcmaModuleService;
import com.hims.bcma.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class BcmaControllerTest {

    @Mock private BcmaModuleService service;
    @InjectMocks private BcmaController controller;

    @Test
    void scanMedicationReturnsOk() {
        PatientDrugMatch match = new PatientDrugMatch(1L,"D1","A",true,"MATCH","ok",10L);
        when(service.scanMedication(anyLong(),anyString(),any(),any(),any())).thenReturn(match);
        ResponseEntity<PatientDrugMatch> res = controller.scanMedication(1L,"D1","A",1L,2L);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void recordIVPReturnsOk() {
        IVPEvent ev = new IVPEvent(); ev.setIvpId(1L); ev.setDrugName("Morphine");
        when(service.recordIVP(anyLong(),anyString(),any(),any(),any(),any(),any(),any())).thenReturn(ev);
        ResponseEntity<IVPEvent> res = controller.recordIVP(1L,"Morphine","5mg","10ml/min",Timestamp.from(Instant.now()),null,5L,2L);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void logExceptionReturnsOk() {
        BCMAException exc = new BCMAException(); exc.setExceptionId(1L); exc.setExceptionType("REFUSED");
        when(service.logException(anyLong(),anyString(),any(),anyString(),any(),any(),any(),any())).thenReturn(exc);
        ResponseEntity<BCMAException> res = controller.logException(1L,"D1",BCMAException.ExceptionType.REFUSED,"reason",9L,null,null,null);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void getStockReturnsList() {
        when(service.getStockByMedication(anyString(),any())).thenReturn(List.of(new MedicationStock()));
        ResponseEntity<List<MedicationStock>> res = controller.getStock("D1",1L);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void getScanReturnsNotFound() {
        when(service.getScanResult(anyLong())).thenReturn(null);
        ResponseEntity<PatientDrugMatch> res = controller.getScan(999L);
        assertEquals(404, res.getStatusCode().value());
    }
}
