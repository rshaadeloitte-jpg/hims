package com.hims.rmrs.domain.repository;

import com.hims.rmrs.domain.model.AccountingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {

    List<AccountingEntry> findByCollectionIdOrderByEntryIdAsc(Long collectionId);

    /**
     * Post-save balance verification inside the same tx:
     * returns one row [SUM(debit), SUM(credit)] for a collection ([null, null]
     * when no rows; callers treat null as 0). Declared as List<Object[]> because
     * a multi-select JPQL query always yields Object[] rows.
     */
    @Query("""
            SELECT SUM(e.debit), SUM(e.credit)
            FROM AccountingEntry e
            WHERE e.collectionId = :collectionId
            """)
    List<Object[]> sumsByCollectionId(@Param("collectionId") long collectionId);
}
