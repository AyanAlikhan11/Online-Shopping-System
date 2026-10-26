package com.ayan.shop.repository;

import com.ayan.shop.model.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Purchases extends JpaRepository<Purchase, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> locked(@Param("id") Long id);

    List<Purchase> findByCustomerIdOrderByCreatedAtDesc(Long id);
}
