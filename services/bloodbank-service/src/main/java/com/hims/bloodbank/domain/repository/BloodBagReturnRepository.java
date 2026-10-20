package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodBagReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;


public interface BloodBagReturnRepository extends JpaRepository<BloodBagReturn, Long> {
    List<BloodBagReturn> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<BloodBagReturn> findByBagIdOrderByAddedDatetimeDesc(Long bagId);

    @Query(value = "SELECT nextval('ihms_bloodbank.t_bloodbag_return')", nativeQuery = true)
    long nextId();
}
