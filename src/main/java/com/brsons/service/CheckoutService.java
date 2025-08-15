package com.brsons.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CartProductEntryRepo;

@Service
public class CheckoutService {
    
    private final CartProductEntryRepo addToCartRepository;

    public CheckoutService(CartProductEntryRepo addToCartRepository) {
        this.addToCartRepository = addToCartRepository;
    }
    @Transactional
    public void clearCart(String userPhone) {
        addToCartRepository.deleteByUserPhone(userPhone);
    }
}
