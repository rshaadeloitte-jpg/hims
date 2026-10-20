package com.hims.waste.domain.repository;

import com.hims.waste.domain.model.BioMedicalWasteDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BioMedicalWasteDetailsRepository extends JpaRepository<BioMedicalWasteDetails, Long> {

    List<BioMedicalWasteDetails> findByBioMedicalWasteIdOrderByIdAsc(Long bioMedicalWasteId);

    Optional<BioMedicalWasteDetails> findByBioMedicalWasteIdAndBarcodeNo(Long bioMedicalWasteId, String barcodeNo);

    /** Minimum bag status of a request's bags (legacy updateBagStatus: request status = min of bag statuses). */
    @Query("""
            SELECT MIN(d.bagStatus) FROM BioMedicalWasteDetails d
            WHERE d.bioMedicalWasteId = :requestId
            """)
    Optional<Long> minBagStatus(@Param("requestId") Long requestId);
}
