package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodBankCrossUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;


public interface BloodBankCrossUnitRepository extends JpaRepository<BloodBankCrossUnit, Long> {
    List<BloodBankCrossUnit> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<BloodBankCrossUnit> findByToBloodbankIdOrderByAddedDatetimeDesc(Long toBloodbankId);
    List<BloodBankCrossUnit> findByFromBloodbankIdOrderByAddedDatetimeDesc(Long fromBloodbankId);

    @Query(value = "SELECT nextval('ihms_bloodbank.t_bloodbank_cross_unit')", nativeQuery = true)
    long nextId();
}
