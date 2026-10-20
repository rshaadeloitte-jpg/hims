package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodDonor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public interface BloodDonorRepository extends JpaRepository<BloodDonor, Long> {

    List<BloodDonor> findByDonationStatus(Integer donationStatus);

    /** BR-BLD-005 scan: DNR_NEW/DNR_FIT donors whose last action is at/before the cutoff. */
    List<BloodDonor> findByDonationStatusInAndLastActionAtLessThanEqual(Collection<Integer> statuses,
                                                                        Timestamp cutoff);
}
