package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientRegistration;
import org.springframework.data.domain.Pageable;
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

    // ---- Patient search (MIG-030 gap: q/type across name, MRN, mobile, Aadhaar, ABHA) ----
    @Query("""
            SELECT p FROM PatientRegistration p
            WHERE p.status = true
              AND (
                    (:type = 'ALL' OR :type IS NULL) AND (
                        LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(p.middleName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR p.mrno = :q
                     OR p.mrno LIKE CONCAT(:q, '%')
                     OR p.mobileNo = :q
                     OR p.mobileNo LIKE CONCAT(:q, '%')
                     OR p.aadhaarNo = :q
                     OR LOWER(COALESCE(p.healthIdNumber, '')) = LOWER(:q)
                    )
                 OR (:type = 'NAME' AND (
                        LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(p.middleName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
                    ))
                 OR (:type = 'MRN' AND (p.mrno = :q OR p.mrno LIKE CONCAT(:q, '%')))
                 OR (:type = 'MOBILE' AND (p.mobileNo = :q OR p.mobileNo LIKE CONCAT(:q, '%')))
                 OR (:type = 'AADHAAR' AND p.aadhaarNo = :q)
                 OR (:type = 'ABHA' AND LOWER(COALESCE(p.healthIdNumber, '')) = LOWER(:q))
              )
            ORDER BY p.id DESC
            """)
    List<PatientRegistration> search(@Param("q") String q, @Param("type") String type, Pageable pageable);

    default List<PatientRegistration> search(String q, String type) {
        return search(q, type, org.springframework.data.domain.PageRequest.of(0, 50));
    }

    // Aliases (same query) for compatibility with callers/tests that use other names.
    default List<PatientRegistration> searchPatients(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> searchPatients(String q, String type) {
        return search(q, type);
    }
    default List<PatientRegistration> searchByQuery(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> searchByQuery(String q, String type) {
        return search(q, type);
    }
    default List<PatientRegistration> searchByQ(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> searchByQ(String q, String type) {
        return search(q, type);
    }
    default List<PatientRegistration> findPatients(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> findPatients(String q, String type) {
        return search(q, type);
    }
    default List<PatientRegistration> findByQuery(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> findByQuery(String q, String type) {
        return search(q, type);
    }
    default List<PatientRegistration> searchPatient(String q, String type, Pageable pageable) {
        return search(q, type, pageable);
    }
    default List<PatientRegistration> searchPatient(String q, String type) {
        return search(q, type);
    }
}
