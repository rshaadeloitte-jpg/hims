package com.hims.ordering.domain.repository;

import com.hims.ordering.domain.model.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaStateRepository extends JpaRepository<SagaState, Long> {
    Optional<SagaState> findByOrderId(Long orderId);
}
