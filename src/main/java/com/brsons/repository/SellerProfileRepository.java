package com.brsons.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.brsons.model.SellerProfile;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    Optional<SellerProfile> findTopByOrderByIdAsc();
}
