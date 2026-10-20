package com.hims.ordering.domain.repository;

import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.OrderItemKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemKey> {
    List<OrderItem> findByOrderIdOrderByLineNoAsc(Long orderId);
}
