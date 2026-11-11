package com.hims.item.domain.repository;

import com.hims.item.domain.model.Indent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IndentRepository extends JpaRepository<Indent, Long> {
    List<Indent> findByDocStatus(Short docStatus);
    List<Indent> findAllByOrderByIdDesc();

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_indent_seq\"')", nativeQuery = true)
    long nextIndentId();
}
