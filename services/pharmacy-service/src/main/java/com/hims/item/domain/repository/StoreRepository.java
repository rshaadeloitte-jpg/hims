package com.hims.item.domain.repository;

import com.hims.item.domain.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
