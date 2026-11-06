package com.hims.cssd.domain.repository;

import com.hims.cssd.domain.model.KitItem;
import com.hims.cssd.domain.model.KitItemKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitItemRepository extends JpaRepository<KitItem, KitItemKey> {

    List<KitItem> findByKitId(Long kitId);
}
