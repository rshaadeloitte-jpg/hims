package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndStatusTrue(Long doctorId);
}
