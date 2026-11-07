package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.Icd11Mms;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Icd11Repository extends JpaRepository<Icd11Mms, Long> {

    Optional<Icd11Mms> findByCodeIgnoreCase(String code);

    @Query("""
        SELECT i FROM Icd11Mms i
        ORDER BY
            CASE WHEN (i.classKind = 'category' AND i.code IS NOT NULL) THEN 0 ELSE 1 END,
            i.code ASC NULLS LAST,
            i.title ASC
    """)
    List<Icd11Mms> findAllOrdered(Pageable pageable);

    @Query("""
        SELECT i FROM Icd11Mms i
        WHERE LOWER(i.code) LIKE LOWER(CONCAT(:q, '%'))
           OR LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY
            CASE WHEN (i.classKind = 'category' AND i.code IS NOT NULL) THEN 0 ELSE 1 END,
            CASE WHEN LOWER(i.code) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END,
            i.code ASC NULLS LAST,
            i.title ASC
    """)
    List<Icd11Mms> searchByQuery(@Param("q") String q, Pageable pageable);
}
