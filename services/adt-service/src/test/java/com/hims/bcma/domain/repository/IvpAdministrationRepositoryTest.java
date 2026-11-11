package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.IvpAdministration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class IvpAdministrationRepositoryTest {
    @Test
    void extendsJpa() {
        assertThat(IvpAdministrationRepository.class.getInterfaces()).contains(JpaRepository.class);
    }
}
