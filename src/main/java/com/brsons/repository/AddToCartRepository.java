package com.brsons.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brsons.model.AddToCart;

public interface AddToCartRepository extends JpaRepository<AddToCart, Long> {
    Optional<AddToCart> findByUserId(Long userId);
}
