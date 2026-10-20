package com.hims.queue.domain.repository;

import com.hims.queue.domain.model.DailyCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCounterRepository extends JpaRepository<DailyCounter, Long> {
}
