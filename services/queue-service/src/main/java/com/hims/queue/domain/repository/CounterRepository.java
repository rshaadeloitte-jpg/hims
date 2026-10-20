package com.hims.queue.domain.repository;

import com.hims.queue.domain.model.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByStatusTrueOrderByIdAsc();

    List<Counter> findByDepartmentIdAndStatusTrueOrderByIdAsc(Long departmentId);

    List<Counter> findByDepartmentId(Long departmentId);
}
