package com.brsons.repository;

import com.brsons.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    // Find by supplier code
    Optional<Supplier> findBySupplierCode(String supplierCode);
    
    // Find by company name (case-insensitive)
    List<Supplier> findByCompanyNameContainingIgnoreCase(String companyName);
    
    // Find by contact person (case-insensitive)
    List<Supplier> findByContactPersonContainingIgnoreCase(String contactPerson);
    
    // Find by email (case-insensitive)
    List<Supplier> findByEmailContainingIgnoreCase(String email);
    
    // Find by phone
    List<Supplier> findByPhoneContaining(String phone);
    
    // Find by status
    List<Supplier> findByStatus(Supplier.SupplierStatus status);
    
    // Find suppliers with credit limit exceeded
    @Query("SELECT s FROM Supplier s WHERE s.creditLimit IS NOT NULL AND s.currentBalance >= s.creditLimit")
    List<Supplier> findSuppliersWithExceededCreditLimit();
    
    // Find suppliers with low credit (less than 20% remaining)
    @Query("SELECT s FROM Supplier s WHERE s.creditLimit IS NOT NULL AND s.currentBalance >= (s.creditLimit * 0.8)")
    List<Supplier> findSuppliersWithLowCredit();
    
    // Search suppliers by multiple criteria
    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.phone LIKE CONCAT('%', :query, '%')")
    List<Supplier> searchSuppliers(@Param("query") String query);
    
    // Find suppliers by city
    List<Supplier> findByCityIgnoreCase(String city);
    
    // Find suppliers by state
    List<Supplier> findByStateIgnoreCase(String state);
    
    // Find suppliers by country
    List<Supplier> findByCountryIgnoreCase(String country);
    
    // Find suppliers with rating above threshold
    List<Supplier> findByRatingGreaterThanEqual(Integer minRating);
    
    // Find suppliers created in date range
    @Query("SELECT s FROM Supplier s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<Supplier> findSuppliersCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                             @Param("endDate") java.time.LocalDateTime endDate);
    
    // Count suppliers by status
    long countByStatus(Supplier.SupplierStatus status);
    
    // Find suppliers with balance above threshold
    @Query("SELECT s FROM Supplier s WHERE s.currentBalance > :minBalance")
    List<Supplier> findSuppliersWithBalanceAbove(@Param("minBalance") Double minBalance);
    
    // Find suppliers with balance below threshold
    @Query("SELECT s FROM Supplier s WHERE s.currentBalance < :maxBalance")
    List<Supplier> findSuppliersWithBalanceBelow(@Param("maxBalance") Double maxBalance);
}
