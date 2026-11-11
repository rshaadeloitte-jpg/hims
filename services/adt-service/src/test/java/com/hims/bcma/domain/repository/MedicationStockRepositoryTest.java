package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.MedicationStock;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationStockRepositoryTest {
    @Test
    void extendsJpa() {
        assertThat(MedicationStockRepository.class.getInterfaces()).contains(JpaRepository.class);
    }
}
