package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodBagDiscard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;


public interface BloodBagDiscardRepository extends JpaRepository<BloodBagDiscard, Long> {
    List<BloodBagDiscard> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<BloodBagDiscard> findByBagIdOrderByAddedDatetimeDesc(Long bagId);

    @Query(value = "SELECT nextval('ihms_bloodbank.t_bloodbag_discard')", nativeQuery = true)
    long nextId();
}
