package com.hims.item.domain.repository;

import com.hims.item.domain.model.ItemReorderPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemReorderPolicyRepository extends JpaRepository<ItemReorderPolicy, Long> {
    List<ItemReorderPolicy> findByStoreId(Long storeId);
    Optional<ItemReorderPolicy> findByStoreIdAndItemId(Long storeId, Long itemId);

    @Query(value = "SELECT nextval('ihms_item.item_reorder_policy_seq')", nativeQuery = true)
    long nextPolicyId();
}
