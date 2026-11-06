package com.hims.mortuary.domain.repository;

import com.hims.mortuary.domain.model.ReleaseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseCertificateRepository extends JpaRepository<ReleaseCertificate, Long> {

    List<ReleaseCertificate> findByDeadBodyId(Long deadBodyId);

    List<ReleaseCertificate> findByReleaseStatus(Short releaseStatus);

    Optional<ReleaseCertificate> findByReleaseNo(String releaseNo);

    @Query(value = "SELECT nextval('ihms_mortuary.t_release_certificate_seq')", nativeQuery = true)
    long nextId();
}
