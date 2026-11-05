package com.ayan.shop.repository;

import com.ayan.shop.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByPurchaseIdOrderByCreatedAtAsc(Long id);
}
