package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.PathOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PathOrderDetailRepository extends JpaRepository<PathOrderDetail, Long> {

    List<PathOrderDetail> findByBookingIdOrderByDetailIdAsc(Long bookingId);

    long countByBookingIdAndIsFinalFalse(Long bookingId);

    long countByBookingId(Long bookingId);
}
