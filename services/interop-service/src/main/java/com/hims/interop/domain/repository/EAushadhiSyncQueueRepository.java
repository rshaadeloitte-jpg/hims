package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.EAushadhiSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EAushadhiSyncQueueRepository extends JpaRepository<EAushadhiSyncQueue, Long> {
    List<EAushadhiSyncQueue> findBySyncStatusOrderByIdDesc(Short syncStatus);
    List<EAushadhiSyncQueue> findAllByOrderByIdDesc();
    @Query(value = "SELECT nextval('ihms_interop.t_eaushadhi_sync_queue_seq')", nativeQuery = true)
    long nextId();
}
