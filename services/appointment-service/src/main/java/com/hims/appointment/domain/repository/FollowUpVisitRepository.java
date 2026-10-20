package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.FollowUpVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FollowUpVisitRepository extends JpaRepository<FollowUpVisit, Long> {

    List<FollowUpVisit> findByParentAppointmentId(Long parentAppointmentId);

    List<FollowUpVisit> findByParentEncounterId(Long parentEncounterId);

    List<FollowUpVisit> findByPatientIdAndFollowUpDateOrderByFollowUpDateAsc(Long patientId, LocalDate date);

    List<FollowUpVisit> findByDoctorIdAndFollowUpDateOrderByFollowUpDateAsc(Long doctorId, LocalDate date);

    List<FollowUpVisit> findByParentFollowUpId(Long parentFollowUpId);

    List<FollowUpVisit> findByFollowUpStatusAndStatusTrue(Short status);
}
