package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.EAushadhiSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EAushadhiSyncStatusRepository extends JpaRepository<EAushadhiSyncStatus, Long> {
    List<EAushadhiSyncStatus> findBySyncQueueIdOrderByTsAsc(Long syncQueueId);
    @Query(value = "SELECT nextval('ihms_interop.t_eaushadhi_sync_status_seq')", nativeQuery = true)
    long nextId();
}
