package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {

    /** WP-S1 saga: the DRAFT produced by one E-13 envelope eventId (partial-UNIQUE column). */
    java.util.Optional<Bill> findBySagaEventId(String sagaEventId);
}
