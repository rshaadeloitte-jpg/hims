package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.WardPicklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardPicklistRepository extends JpaRepository<WardPicklist, Long> {
}
