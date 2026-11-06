package com.hims.linen.domain.repository;

import com.hims.linen.domain.model.LinenRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface LinenRequestRepository extends JpaRepository<LinenRequest, Long> {

    /**
     * Legacy getCurrentLinenStatus search semantics: the request is visible to a
     * store when it is either the source (store_id) or the destination
     * (to_store_id); optional category filter maps to request_status; search is
     * a case-insensitive LIKE on requisition_no; optional request-date range
     * [fromDate, toDateExclusive). Newest first.
     *
     * <p>All filter params are gated by booleans and never bound as null:
     * PostgreSQL cannot infer the type of an untyped null parameter (binds as
     * bytea and breaks UPPER()/= comparisons), so the service passes sentinel
     * values (0L / "" / epoch) that are ignored when the matching gate is false.
     */
    @Query("""
            SELECT r FROM LinenRequest r
            WHERE r.status = true
              AND (:hasUnit = false OR r.unitId = :unitId)
              AND (:hasStore = false OR r.storeId = :storeId OR r.toStoreId = :storeId)
              AND (:hasStatus = false OR r.requestStatus = :requestStatus)
              AND (:hasSearch = false
                   OR UPPER(r.requisitionNo) LIKE CONCAT('%', UPPER(:search), '%'))
              AND (:hasFrom = false OR r.requestDate >= :fromDate)
              AND (:hasTo = false OR r.requestDate < :toDateExclusive)
            ORDER BY r.id DESC
            """)
    List<LinenRequest> search(@Param("unitId") Long unitId,
                              @Param("hasUnit") boolean hasUnit,
                              @Param("storeId") Long storeId,
                              @Param("hasStore") boolean hasStore,
                              @Param("requestStatus") Long requestStatus,
                              @Param("hasStatus") boolean hasStatus,
                              @Param("search") String search,
                              @Param("hasSearch") boolean hasSearch,
                              @Param("fromDate") Timestamp fromDate,
                              @Param("hasFrom") boolean hasFrom,
                              @Param("toDateExclusive") Timestamp toDateExclusive,
                              @Param("hasTo") boolean hasTo);

    /**
     * Legacy getLinenRequestCount: count scoped by unit + store (source or
     * destination) + optional request-date range + optional request_status.
     * Same null-sentinel gating as {@link #search}.
     */
    @Query("""
            SELECT COUNT(r) FROM LinenRequest r
            WHERE r.status = true
              AND (:hasUnit = false OR r.unitId = :unitId)
              AND (:hasStore = false OR r.storeId = :storeId OR r.toStoreId = :storeId)
              AND (:hasFrom = false OR r.requestDate >= :fromDate)
              AND (:hasTo = false OR r.requestDate < :toDateExclusive)
              AND (:hasStatus = false OR r.requestStatus = :requestStatus)
            """)
    long countScoped(@Param("unitId") Long unitId,
                     @Param("hasUnit") boolean hasUnit,
                     @Param("storeId") Long storeId,
                     @Param("hasStore") boolean hasStore,
                     @Param("fromDate") Timestamp fromDate,
                     @Param("hasFrom") boolean hasFrom,
                     @Param("toDateExclusive") Timestamp toDateExclusive,
                     @Param("hasTo") boolean hasTo,
                     @Param("requestStatus") Long requestStatus,
                     @Param("hasStatus") boolean hasStatus);
}
