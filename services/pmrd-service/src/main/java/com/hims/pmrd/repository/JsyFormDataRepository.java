package com.hims.pmrd.repository;
import com.hims.pmrd.domain.model.JsyFormData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JsyFormDataRepository extends JpaRepository<JsyFormData, Long> {
    List<JsyFormData> findByPatientIdAndFormType(Long patientId, String formType);
}
