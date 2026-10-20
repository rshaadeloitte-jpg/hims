package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.IvCompoundLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IvCompoundLineRepository extends JpaRepository<IvCompoundLine, Long> {

    @Query("SELECT l FROM IvCompoundLine l WHERE l.compoundId = :compoundId ORDER BY l.lineNo ASC")
    List<IvCompoundLine> findByCompoundIdOrderByLineNo(@Param("compoundId") Long compoundId);
}
