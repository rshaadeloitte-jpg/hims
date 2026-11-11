package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtScheduleDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtScheduleDetailRepository extends JpaRepository<OtScheduleDetail, Long> {

    List<OtScheduleDetail> findByScheduleIdOrderByDetailIdAsc(Long scheduleId);
}
