package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentComplaintRepository extends JpaRepository<EquipmentComplaint, Long> {

    /** Complaint history of one asset — newest first. */
    List<EquipmentComplaint> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /** Complaint-number lookup (IHMS/EQ/...), case-insensitive. */
    Optional<EquipmentComplaint> findByComplaintNoIgnoreCase(String complaintNo);

    /**
     * Complaint search. All filter params are gated by booleans and never bound
     * as null: PostgreSQL cannot infer the type of an untyped null parameter
     * (binds as bytea and breaks UPPER()/= comparisons), so the service passes
     * sentinel values (0L / "") that are ignored when the matching gate is false
     * (same convention as EquipmentRepository.search).
     *
     * <p>Free-text search matches complaint_no, description and accession_no.
     */
    @Query("""
            SELECT c FROM EquipmentComplaint c
            WHERE c.status = true
              AND (:hasUnit = false OR c.unitId = :unitId)
              AND (:hasEquipment = false OR c.equipmentId = :equipmentId)
              AND (:hasStatus = false OR c.complaintStatus = :complaintStatus)
              AND (:hasSearch = false
                   OR UPPER(c.complaintNo) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(c.description) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(c.accessionNo) LIKE CONCAT('%', UPPER(:search), '%'))
            ORDER BY c.id DESC
            """)
    List<EquipmentComplaint> search(@Param("unitId") Long unitId,
                                    @Param("hasUnit") boolean hasUnit,
                                    @Param("equipmentId") Long equipmentId,
                                    @Param("hasEquipment") boolean hasEquipment,
                                    @Param("complaintStatus") Long complaintStatus,
                                    @Param("hasStatus") boolean hasStatus,
                                    @Param("search") String search,
                                    @Param("hasSearch") boolean hasSearch);
}
