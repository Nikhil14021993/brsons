package com.brsons.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.brsons.model.AddToCart;
import com.brsons.model.CartProductEntry;
import com.brsons.model.CartProductEntry1;

public interface CartProductEntryRepo extends JpaRepository<CartProductEntry1, Long> {
    List<CartProductEntry1> findByUserPhone(String userPhone);
    
    void deleteByUserPhone(String userPhone);
}
