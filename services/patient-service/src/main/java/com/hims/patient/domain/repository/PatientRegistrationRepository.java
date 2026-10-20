package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRegistrationRepository extends JpaRepository<PatientRegistration, Long> {

    Optional<PatientRegistration> findByMrno(String mrno);

    // ---- BR-PAT-002 duplicate detection matrix ----------------------------

    Optional<PatientRegistration> findFirstByHealthIdNumberIgnoreCase(String healthIdNumber);

    Optional<PatientRegistration> findFirstByAadhaarNo(String aadhaarNo);

    Optional<PatientRegistration> findFirstByVid(String vid);

    Optional<PatientRegistration> findFirstByJanaadhaarId(String janaadhaarId);

    List<PatientRegistration> findByMobileNoAndStatusTrueOrderByIdAsc(String mobileNo);

    /** Legacy heuristic: same mobile + same first name + same DOB → identity hit. */
    @Query("""
            SELECT p FROM PatientRegistration p
            WHERE p.mobileNo = :mobile
              AND LOWER(p.firstName) = LOWER(:firstName)
              AND p.dob = :dob
            """)
    List<PatientRegistration> findIdentityHits(@Param("mobile") String mobile,
                                               @Param("firstName") String firstName,
                                               @Param("dob") LocalDate dob);

    /** Same heuristic when the candidate has no DOB on record. */
    @Query("""
            SELECT p FROM PatientRegistration p
            WHERE p.mobileNo = :mobile
              AND LOWER(p.firstName) = LOWER(:firstName)
              AND p.dob IS NULL
            """)
    List<PatientRegistration> findIdentityHitsWithoutDob(@Param("mobile") String mobile,
                                                         @Param("firstName") String firstName);

    // ---- M01: COVID tracking ------------------------------------------------
    List<PatientRegistration> findByCovidStatus(String covidStatus);

    @Query("SELECT p FROM PatientRegistration p WHERE p.covidStatus IS NOT NULL")
    List<PatientRegistration> findAllCovidTracked();

    @Query("SELECT p FROM PatientRegistration p WHERE p.covidStatus = :status")
    List<PatientRegistration> findByCovidStatusIgnoreCase(@Param("status") String status);
}
