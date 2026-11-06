package com.hims.rmrs.domain.repository;
import com.hims.rmrs.domain.model.PoRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PoRequestRepository extends JpaRepository<PoRequest, Long> {
    List<PoRequest> findByDocStatusOrderByAddedDatetimeDesc(Short status);
    @Query(value = "SELECT nextval('ihms_rmrs.t_po_request_seq')", nativeQuery = true)
    long nextId();
}
