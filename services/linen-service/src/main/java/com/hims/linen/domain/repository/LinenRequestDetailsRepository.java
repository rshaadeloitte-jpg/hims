package com.hims.linen.domain.repository;

import com.hims.linen.domain.model.LinenRequestDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinenRequestDetailsRepository extends JpaRepository<LinenRequestDetails, Long> {

    List<LinenRequestDetails> findByLinenRequestIdOrderByIdAsc(Long linenRequestId);
}
