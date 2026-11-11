package com.hims.bcma.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationStockTest {

    @Test
    void stockFields() {
        MedicationStock s = new MedicationStock();
        s.setStockId(1L); s.setDrugNdc("NDC-1"); s.setBatchId("B01");
        s.setQuantityOnHand(new java.math.BigDecimal("50"));
        assertThat(s.getBatchId()).isEqualTo("B01");
    }
}
