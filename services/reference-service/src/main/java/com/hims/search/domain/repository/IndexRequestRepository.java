package com.hims.search.domain.repository;

import com.hims.search.domain.model.IndexRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Read/write access to the t_index_request journal. Dedupe uses the LATEST-ROW
 * query per register WP-T3 — UNIQUE(collection,doc_id,action,status) is
 * deliberately NOT enforced so that a FAILED row can be superseded by a fresh
 * PENDING attempt.
 */
public interface IndexRequestRepository extends JpaRepository<IndexRequest, Long> {

    /** Latest journal row for a (collection, doc_id, action) key — dedupe basis. */
    Optional<IndexRequest> findTopByCollectionAndDocIdAndActionOrderByIdDesc(
            String collection, String docId, short action);

    /** Filtered listing for GET /api/v1/index-requests?collection=&amp;status=. */
    @Query("""
            SELECT r FROM IndexRequest r
            WHERE (:collection IS NULL OR r.collection = :collection)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.id DESC
            """)
    List<IndexRequest> findByFilters(@Param("collection") String collection,
                                     @Param("status") Short status);
}
