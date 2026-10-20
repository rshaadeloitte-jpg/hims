package com.hims.cssd.domain.repository;

import com.hims.cssd.domain.model.Kit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KitRepository extends JpaRepository<Kit, Long> {

    boolean existsByKitCode(String kitCode);

    /** List: optional status and procedure-type filters, newest first. */
    @Query("""
            SELECT k FROM Kit k
            WHERE (:status IS NULL OR k.status = :status)
              AND (:procedureType IS NULL OR LOWER(k.procedureType) = LOWER(:procedureType))
            ORDER BY k.kitId DESC
            """)
    List<Kit> search(@Param("status") Short status, @Param("procedureType") String procedureType);
}
