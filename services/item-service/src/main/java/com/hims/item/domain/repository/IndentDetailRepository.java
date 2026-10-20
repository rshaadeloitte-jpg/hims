package com.hims.item.domain.repository;

import com.hims.item.domain.model.IndentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IndentDetailRepository extends JpaRepository<IndentDetail, Long> {
    List<IndentDetail> findByIndentId(Long indentId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_indent_detail_seq\"')", nativeQuery = true)
    long nextIndentDetailId();
}
