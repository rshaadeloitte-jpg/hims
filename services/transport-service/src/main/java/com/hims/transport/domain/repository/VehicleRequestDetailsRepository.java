package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.VehicleRequestDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRequestDetailsRepository extends JpaRepository<VehicleRequestDetails, Long> {

    /** Legacy getDetailsForVehicleFare / estimated-fare lookup: newest first. */
    List<VehicleRequestDetails> findByVehicleRequestIdOrderByIdDesc(Long vehicleRequestId);

    List<VehicleRequestDetails> findByVehicleRequestIdOrderByIdAsc(Long vehicleRequestId);
}
