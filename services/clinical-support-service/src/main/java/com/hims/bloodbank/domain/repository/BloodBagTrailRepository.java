package com.hims.bloodbank.domain.repository;
import com.hims.bloodbank.domain.model.BloodBagTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BloodBagTrailRepository extends JpaRepository<BloodBagTrail, Long> {
    List<BloodBagTrail> findByBagIdOrderByEventAtAsc(Long bagId);
    @Query(value = "SELECT nextval('ihms_bloodbank.t_bloodbag_trail_seq')", nativeQuery = true)
    long nextId();
}
