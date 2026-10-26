package com.ayan.shop.repository;

import com.ayan.shop.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Favorites extends JpaRepository<Favorite, Long> {
    @Modifying
    @Query("delete from Favorite f where f.product.id = :id")
    void deleteByProductId(@Param("id") Long id);

    @Modifying
    @Query("delete from Favorite f where f.customer.id = :id")
    void deleteByCustomerId(@Param("id") Long id);

    List<Favorite> findByCustomerId(Long id);

    Optional<Favorite> findByCustomerIdAndProductId(Long cid, Long pid);
}
