package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.NhcxRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NhcxRequestRepository extends JpaRepository<NhcxRequest, Long> {

    java.util.Optional<NhcxRequest> findByCorrelationId(String correlationId);

    /** List: optional kind and status filters, newest first. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM NhcxRequest r
            WHERE (:kind IS NULL OR r.kind = :kind)
              AND (:status IS NULL OR r.statusCode = :status)
            ORDER BY r.requestId DESC
            """)
    List<NhcxRequest> search(@org.springframework.data.repository.query.Param("kind") Short kind,
                             @org.springframework.data.repository.query.Param("status") Short status);
}
