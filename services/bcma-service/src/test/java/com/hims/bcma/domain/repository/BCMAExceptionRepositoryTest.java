package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.BCMAException;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class BCMAExceptionRepositoryTest {
    @Test
    void extendsJpa() {
        assertThat(BCMAExceptionRepository.class.getInterfaces()).contains(JpaRepository.class);
    }
}
