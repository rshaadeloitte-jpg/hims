package com.hims.mortuary.domain.repository;

import com.hims.mortuary.domain.model.AutopsyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutopsyRequestRepository extends JpaRepository<AutopsyRequest, Long> {

    List<AutopsyRequest> findByDeadBodyId(Long deadBodyId);

    List<AutopsyRequest> findByRequestStatus(Short requestStatus);

    @Query(value = "SELECT nextval('ihms_mortuary.t_autopsy_request_seq')", nativeQuery = true)
    long nextId();
}
