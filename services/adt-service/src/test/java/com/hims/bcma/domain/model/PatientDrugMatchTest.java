package com.hims.bcma.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PatientDrugMatchTest {

    @Test
    void isMatchReturnsTrueForMatchStatus() {
        PatientDrugMatch m = new PatientDrugMatch(1L,"D1","A",true,"MATCH","ok",10L);
        assertTrue(m.isMatch());
    }

    @Test
    void isMatchReturnsTrueWhenVerifiedTrue() {
        PatientDrugMatch m = new PatientDrugMatch(2L,"D2","B",true,"PENDING","ok",11L);
        assertTrue(m.isMatch());
    }

    @Test
    void isMatchReturnsFalseWhenNotMatch() {
        PatientDrugMatch m = new PatientDrugMatch(3L,"D3","C",false,"MISMATCH","no",12L);
        assertFalse(m.isMatch());
    }

    @Test
    void recordFields() {
        PatientDrugMatch m = new PatientDrugMatch(4L,"D4","E",true,"MATCH","msg",13L);
        assertEquals(4L, m.patientId());
        assertEquals("D4", m.drugIen());
        assertEquals("E", m.drugName());
    }
}
