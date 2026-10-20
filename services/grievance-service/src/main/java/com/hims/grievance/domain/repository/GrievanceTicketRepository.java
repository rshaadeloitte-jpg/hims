package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface GrievanceTicketRepository extends JpaRepository<GrievanceTicket, Long> {

    List<GrievanceTicket> findByMobileNoAndStatusTrueOrderByIdDesc(Long mobileNo);

    List<GrievanceTicket> findByUnitIdAndStatusTrueOrderByIdDesc(Long unitId);

    List<GrievanceTicket> findByStatusTrueOrderByIdDesc();

    long countByStatusTrue();

    long countByStatusTrueAndIssueStatus(Short issueStatus);

    long countByStatusTrueAndIsCitizenTrue();

    @Query("""
            SELECT COUNT(t) FROM GrievanceTicket t
            WHERE t.status = true AND t.issueStatus NOT IN (3, 4) AND t.assignedUserId = :employeeId
            """)
    long countMyList(@Param("employeeId") Long employeeId);

    @Query("""
            SELECT COUNT(t) FROM GrievanceTicket t
            WHERE t.status = true AND t.issueStatus IN (1, 2)
              AND t.issueDate >= :dayStart AND t.issueDate < :dayEnd
            """)
    long countTodays(@Param("dayStart") Timestamp dayStart, @Param("dayEnd") Timestamp dayEnd);
}
