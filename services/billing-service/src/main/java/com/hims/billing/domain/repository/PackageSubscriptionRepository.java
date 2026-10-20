package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.PackageSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageSubscriptionRepository extends JpaRepository<PackageSubscription, Long> {

    List<PackageSubscription> findByPatientIdOrderBySubscriptionIdDesc(Long patientId);

    List<PackageSubscription> findByBillIdOrderBySubscriptionIdAsc(Long billId);
}
