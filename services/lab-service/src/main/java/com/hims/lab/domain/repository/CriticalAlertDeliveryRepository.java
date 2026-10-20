package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.CriticalAlertDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriticalAlertDeliveryRepository extends JpaRepository<CriticalAlertDelivery, Long> {

    List<CriticalAlertDelivery> findByBookingIdOrderByDeliveryIdAsc(Long bookingId);

    List<CriticalAlertDelivery> findByDeliveryStatusInOrderByAddedDatetimeAsc(List<String> statuses);

    List<CriticalAlertDelivery> findByPatientIdOrderByDeliveryIdAsc(Long patientId);
}
