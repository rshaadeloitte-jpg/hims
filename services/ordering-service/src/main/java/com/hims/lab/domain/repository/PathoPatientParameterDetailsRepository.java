package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.PathoPatientParameterDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PathoPatientParameterDetailsRepository extends JpaRepository<PathoPatientParameterDetails, Long> {

    List<PathoPatientParameterDetails> findByDetailIdOrderByParamIdAsc(Long detailId);

    List<PathoPatientParameterDetails> findByDetailIdInOrderByParamIdAsc(List<Long> detailIds);

    Optional<PathoPatientParameterDetails> findByDetailIdAndParameterNameIgnoreCase(Long detailId, String parameterName);

    void deleteByDetailIdAndParamIdNotIn(Long detailId, List<Long> keepParamIds);
}
