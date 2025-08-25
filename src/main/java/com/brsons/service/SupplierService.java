package com.brsons.service;

import com.brsons.model.Supplier;
import com.brsons.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SupplierService {
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    // Create new supplier
    public Supplier createSupplier(Supplier supplier) {
        // Generate unique supplier code if not provided
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().trim().isEmpty()) {
            supplier.setSupplierCode(generateSupplierCode());
        }
        
        // Set default values
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        supplier.setStatus(Supplier.SupplierStatus.ACTIVE);
        supplier.setCurrentBalance(0.0);
        
        // Validate supplier data
        validateSupplier(supplier);
        
        return supplierRepository.save(supplier);
    }
    
    // Update existing supplier
    public Supplier updateSupplier(Long id, Supplier supplierDetails) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            
            // Update fields
            supplier.setCompanyName(supplierDetails.getCompanyName());
            supplier.setContactPerson(supplierDetails.getContactPerson());
            supplier.setEmail(supplierDetails.getEmail());
            supplier.setPhone(supplierDetails.getPhone());
            supplier.setAddressLine1(supplierDetails.getAddressLine1());
            supplier.setAddressLine2(supplierDetails.getAddressLine2());
            supplier.setCity(supplierDetails.getCity());
            supplier.setState(supplierDetails.getState());
            supplier.setZipCode(supplierDetails.getZipCode());
            supplier.setCountry(supplierDetails.getCountry());
            supplier.setGstin(supplierDetails.getGstin());
            supplier.setPan(supplierDetails.getPan());
            supplier.setPaymentTerms(supplierDetails.getPaymentTerms());
            supplier.setCreditLimit(supplierDetails.getCreditLimit());
            supplier.setRating(supplierDetails.getRating());
            supplier.setNotes(supplierDetails.getNotes());
            supplier.setUpdatedAt(LocalDateTime.now());
            
            // Validate supplier data
            validateSupplier(supplier);
            
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Get supplier by ID
    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }
    
    // Get supplier by supplier code
    public Optional<Supplier> getSupplierByCode(String supplierCode) {
        return supplierRepository.findBySupplierCode(supplierCode);
    }
    
    // Get all suppliers
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }
    
    // Get active suppliers
    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findByStatus(Supplier.SupplierStatus.ACTIVE);
    }
    
    // Search suppliers
    public List<Supplier> searchSuppliers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllSuppliers();
        }
        return supplierRepository.searchSuppliers(query.trim());
    }
    
    // Get suppliers by status
    public List<Supplier> getSuppliersByStatus(Supplier.SupplierStatus status) {
        return supplierRepository.findByStatus(status);
    }
    
    // Get suppliers by city
    public List<Supplier> getSuppliersByCity(String city) {
        return supplierRepository.findByCityIgnoreCase(city);
    }
    
    // Get suppliers by state
    public List<Supplier> getSuppliersByState(String state) {
        return supplierRepository.findByStateIgnoreCase(state);
    }
    
    // Get suppliers by country
    public List<Supplier> getSuppliersByCountry(String country) {
        return supplierRepository.findByCountryIgnoreCase(country);
    }
    
    // Get suppliers with exceeded credit limit
    public List<Supplier> getSuppliersWithExceededCreditLimit() {
        return supplierRepository.findSuppliersWithExceededCreditLimit();
    }
    
    // Get suppliers with low credit
    public List<Supplier> getSuppliersWithLowCredit() {
        return supplierRepository.findSuppliersWithLowCredit();
    }
    
    // Get suppliers by rating
    public List<Supplier> getSuppliersByRating(Integer minRating) {
        return supplierRepository.findByRatingGreaterThanEqual(minRating);
    }
    
    // Get suppliers created in date range
    public List<Supplier> getSuppliersCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return supplierRepository.findSuppliersCreatedBetween(startDate, endDate);
    }
    
    // Update supplier status
    public Supplier updateSupplierStatus(Long id, Supplier.SupplierStatus status) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            supplier.setStatus(status);
            supplier.setUpdatedAt(LocalDateTime.now());
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Update supplier balance
    public Supplier updateSupplierBalance(Long id, Double newBalance) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            supplier.setCurrentBalance(newBalance);
            supplier.setUpdatedAt(LocalDateTime.now());
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Add to supplier balance
    public Supplier addToSupplierBalance(Long id, Double amount) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            Double currentBalance = supplier.getCurrentBalance() != null ? supplier.getCurrentBalance() : 0.0;
            supplier.setCurrentBalance(currentBalance + amount);
            supplier.setUpdatedAt(LocalDateTime.now());
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Subtract from supplier balance
    public Supplier subtractFromSupplierBalance(Long id, Double amount) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            Double currentBalance = supplier.getCurrentBalance() != null ? supplier.getCurrentBalance() : 0.0;
            supplier.setCurrentBalance(Math.max(0, currentBalance - amount));
            supplier.setUpdatedAt(LocalDateTime.now());
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Update supplier rating
    public Supplier updateSupplierRating(Long id, Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            supplier.setRating(rating);
            supplier.setUpdatedAt(LocalDateTime.now());
            return supplierRepository.save(supplier);
        }
        throw new RuntimeException("Supplier not found with id: " + id);
    }
    
    // Delete supplier (soft delete by setting status to INACTIVE)
    public Supplier deleteSupplier(Long id) {
        return updateSupplierStatus(id, Supplier.SupplierStatus.INACTIVE);
    }
    
    // Get supplier statistics
    public SupplierStatistics getSupplierStatistics() {
        long totalSuppliers = supplierRepository.count();
        long activeSuppliers = supplierRepository.countByStatus(Supplier.SupplierStatus.ACTIVE);
        long inactiveSuppliers = supplierRepository.countByStatus(Supplier.SupplierStatus.INACTIVE);
        long suspendedSuppliers = supplierRepository.countByStatus(Supplier.SupplierStatus.SUSPENDED);
        
        List<Supplier> exceededCreditLimit = getSuppliersWithExceededCreditLimit();
        List<Supplier> lowCredit = getSuppliersWithLowCredit();
        
        return new SupplierStatistics(
            totalSuppliers,
            activeSuppliers,
            inactiveSuppliers,
            suspendedSuppliers,
            exceededCreditLimit.size(),
            lowCredit.size()
        );
    }
    
    // Validate supplier data
    private void validateSupplier(Supplier supplier) {
        if (supplier.getCompanyName() == null || supplier.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        
        if (supplier.getEmail() == null || supplier.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier code is required");
        }
        
        // Check if supplier code is unique
        Optional<Supplier> existingSupplier = supplierRepository.findBySupplierCode(supplier.getSupplierCode());
        if (existingSupplier.isPresent() && !existingSupplier.get().getId().equals(supplier.getId())) {
            throw new IllegalArgumentException("Supplier code must be unique");
        }
        
        // Check if email is unique
        List<Supplier> existingSuppliers = supplierRepository.findByEmailContainingIgnoreCase(supplier.getEmail());
        for (Supplier existing : existingSuppliers) {
            if (!existing.getId().equals(supplier.getId())) {
                throw new IllegalArgumentException("Email must be unique");
            }
        }
    }
    
    // Generate unique supplier code
    private String generateSupplierCode() {
        String prefix = "SUP";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 4 digits
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }
    
    // Statistics class
    public static class SupplierStatistics {
        private final long totalSuppliers;
        private final long activeSuppliers;
        private final long inactiveSuppliers;
        private final long suspendedSuppliers;
        private final long exceededCreditLimit;
        private final long lowCredit;
        
        public SupplierStatistics(long totalSuppliers, long activeSuppliers, long inactiveSuppliers, 
                                long suspendedSuppliers, long exceededCreditLimit, long lowCredit) {
            this.totalSuppliers = totalSuppliers;
            this.activeSuppliers = activeSuppliers;
            this.inactiveSuppliers = inactiveSuppliers;
            this.suspendedSuppliers = suspendedSuppliers;
            this.exceededCreditLimit = exceededCreditLimit;
            this.lowCredit = lowCredit;
        }
        
        // Getters
        public long getTotalSuppliers() { return totalSuppliers; }
        public long getActiveSuppliers() { return activeSuppliers; }
        public long getInactiveSuppliers() { return inactiveSuppliers; }
        public long getSuspendedSuppliers() { return suspendedSuppliers; }
        public long getExceededCreditLimit() { return exceededCreditLimit; }
        public long getLowCredit() { return lowCredit; }
    }
}
