package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.BcmaVerification;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class BcmaVerificationRepositoryTest {

    @Test
    void repositoryExtendsJpa() {
        assertThat(BcmaVerificationRepository.class.getInterfaces()).contains(JpaRepository.class);
    }
}
