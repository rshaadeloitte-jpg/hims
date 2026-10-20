package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.EhrJobDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EhrJobDetailsRepository extends JpaRepository<EhrJobDetails, Long> {

    List<EhrJobDetails> findByStatusOrderByIdDesc(Short status);

    List<EhrJobDetails> findAllByOrderByIdDesc();
}
