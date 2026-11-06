package com.hims.rmrs.domain.repository;

import com.hims.rmrs.application.ServiceWiseSummaryRow;
import com.hims.rmrs.application.UserWiseSummaryRow;
import com.hims.rmrs.domain.model.RmrsCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RmrsCollectionRepository extends JpaRepository<RmrsCollection, Long> {

    /**
     * Paginated search (?dateFrom=&dateTo=&headId=) — REPLACES the legacy ROWNUM
     * pagination of POSTGRESQL-MIGRATION :1361-1366 with limit-offset via
     * Spring Data {@link Pageable} (page/size, size capped in the controller).
     */
    // COALESCE(col, param) form (instead of ":param IS NULL OR ..."): PG cannot
    // infer the type of an untyped null JDBC parameter, so null date filters blow
    // up with "could not determine data type of parameter". COALESCE against the
    // typed column keeps identical semantics (param null -> predicate always true).
    @Query("""
            SELECT c FROM RmrsCollection c
            WHERE c.headId = COALESCE(:headId, c.headId)
              AND c.collectionDate >= COALESCE(:dateFrom, c.collectionDate)
              AND c.collectionDate <= COALESCE(:dateTo, c.collectionDate)
            ORDER BY c.collectionDate DESC, c.collectionId DESC
            """)
    Page<RmrsCollection> search(@Param("dateFrom") LocalDate dateFrom,
                                @Param("dateTo") LocalDate dateTo,
                                @Param("headId") Long headId,
                                Pageable pageable);

    /** s_servicewisecollection analog: totals grouped by treasury head. */
    @Query("""
            SELECT new com.hims.rmrs.application.ServiceWiseSummaryRow(
                       c.headId, h.headCode, h.headName, SUM(c.amount), COUNT(c))
            FROM RmrsCollection c
            JOIN HeadMaster h ON h.headId = c.headId
            WHERE c.collectionDate >= COALESCE(:dateFrom, c.collectionDate)
              AND c.collectionDate <= COALESCE(:dateTo, c.collectionDate)
            GROUP BY c.headId, h.headCode, h.headName
            ORDER BY SUM(c.amount) DESC
            """)
    java.util.List<ServiceWiseSummaryRow> summaryServiceWise(@Param("dateFrom") LocalDate dateFrom,
                                                             @Param("dateTo") LocalDate dateTo);

    /** s_userwisecollection analog: totals grouped by collecting user. */
    @Query("""
            SELECT new com.hims.rmrs.application.UserWiseSummaryRow(
                       c.collectedBy, SUM(c.amount), COUNT(c))
            FROM RmrsCollection c
            WHERE c.collectionDate >= COALESCE(:dateFrom, c.collectionDate)
              AND c.collectionDate <= COALESCE(:dateTo, c.collectionDate)
            GROUP BY c.collectedBy
            ORDER BY SUM(c.amount) DESC
            """)
    java.util.List<UserWiseSummaryRow> summaryUserWise(@Param("dateFrom") LocalDate dateFrom,
                                                       @Param("dateTo") LocalDate dateTo);

    boolean existsByReceiptNo(String receiptNo);
}
