package com.brsons.repository;

import com.brsons.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
	Optional<Invoice> findByOrder_Id(Long orderId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
