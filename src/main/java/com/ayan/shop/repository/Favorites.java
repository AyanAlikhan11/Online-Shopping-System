package com.ayan.shop.repository;

import com.ayan.shop.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Favorites extends JpaRepository<Favorite, Long> {
    void deleteByProductId(Long id);

    void deleteByCustomerId(Long id);

    List<Favorite> findByCustomerId(Long id);

    Optional<Favorite> findByCustomerIdAndProductId(Long customerId, Long productId);
}
