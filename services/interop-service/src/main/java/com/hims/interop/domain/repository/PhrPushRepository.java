package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.PhrPush;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PhrPushRepository extends JpaRepository<PhrPush, Long> {
    List<PhrPush> findByAbhaLinkIdAndDocStatus(Long abhaLinkId, Short docStatus);
    @Query(value = "SELECT nextval('ihms_interop.t_phr_push')", nativeQuery = true)
    long nextId();
}
