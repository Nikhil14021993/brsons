package com.brsons.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brsons.model.*;
import com.brsons.repository.*;
import java.time.LocalDateTime;
import java.math.RoundingMode;

@Service
public class OrderAccountingService {

    private final ProductRepository productRepository; // assumes exists
    private final SellerProfileRepository sellerRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final InvoiceNumberService invoiceNumberService;
    private final OrderRepository orderRepository;
    private final com.brsons.repository.OutstandingRepository outstandingRepository;
    private final CustomerLedgerService customerLedgerService;
    private final TaxCalculationService taxCalculationService;

    public OrderAccountingService(
        ProductRepository productRepository,
        SellerProfileRepository sellerRepo,
        LedgerEntryRepository ledgerRepo,
        InvoiceNumberService invoiceNumberService,
        OrderRepository orderRepository,
        com.brsons.repository.OutstandingRepository outstandingRepository,
        CustomerLedgerService customerLedgerService,
        TaxCalculationService taxCalculationService
    ) {
        this.productRepository = productRepository;
        this.sellerRepo = sellerRepo;
        this.ledgerRepo = ledgerRepo;
        this.invoiceNumberService = invoiceNumberService;
        this.orderRepository = orderRepository;
        this.outstandingRepository = outstandingRepository;
        this.customerLedgerService = customerLedgerService;
        this.taxCalculationService = taxCalculationService;
    }

    @Transactional
    public void finalizeTotalsAndInvoice(Order order, BigDecimal gstRatePct, String billType, String userType, String userState) {
        // 1) compute subTotal from items using stored prices in OrderItems
        BigDecimal sub = BigDecimal.ZERO;
        List<OrderItem> items = order.getOrderItems();
        if (items != null) {
            for (OrderItem it : items) {
                // Use the stored price from OrderItem (already calculated with correct user pricing)
                if (it.getTotalPrice() != null) {
                    sub = sub.add(it.getTotalPrice());
                } else {
                    // Fallback: calculate from unit price if total price is not set
                    if (it.getUnitPrice() != null) {
                        BigDecimal line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
                        sub = sub.add(line);
                        // Update the total price for this item
                        it.setTotalPrice(line);
                    }
                }
            }
        }

        // 2) Tax calculation - only calculate for non-B2B users
        BigDecimal rate = BigDecimal.ZERO;
        BigDecimal gstAmt = BigDecimal.ZERO;
        
        // Tax breakdown fields
        String taxType = "UNKNOWN";
        BigDecimal cgstRate = BigDecimal.ZERO;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstRate = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstRate = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;
        
        if (!"B2B".equalsIgnoreCase(userType)) {
            // Determine tax type based on user state
            taxType = taxCalculationService.determineTaxType(userState);
            
            if ("CGST_SGST".equals(taxType)) {
                // Intra-state: CGST + SGST
                // Get tax rates from first product (assuming all products have same tax rates)
                if (items != null && !items.isEmpty()) {
                    Product firstProduct = productRepository.findById(items.get(0).getProductId()).orElse(null);
                    if (firstProduct != null) {
                        cgstRate = firstProduct.getCgstPercentage() != null ? firstProduct.getCgstPercentage() : BigDecimal.ZERO;
                        sgstRate = firstProduct.getSgstPercentage() != null ? firstProduct.getSgstPercentage() : BigDecimal.ZERO;
                        
                        cgstAmount = sub.multiply(cgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        sgstAmount = sub.multiply(sgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        
                        gstAmt = cgstAmount.add(sgstAmount);
                        rate = cgstRate.add(sgstRate); // Total rate for backward compatibility
                    }
                }
            } else if ("IGST".equals(taxType)) {
                // Inter-state: IGST
                // Get tax rate from first product
                if (items != null && !items.isEmpty()) {
                    Product firstProduct = productRepository.findById(items.get(0).getProductId()).orElse(null);
                    if (firstProduct != null) {
                        igstRate = firstProduct.getIgstPercentage() != null ? firstProduct.getIgstPercentage() : BigDecimal.ZERO;
                        igstAmount = sub.multiply(igstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        
                        gstAmt = igstAmount;
                        rate = igstRate; // For backward compatibility
                    }
                }
            } else {
                // Fallback to generic GST if tax type is unknown
                rate = gstRatePct == null ? BigDecimal.ZERO : gstRatePct;
                gstAmt = sub.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        // 3) Total
        BigDecimal total = sub.add(gstAmt);

        // 4) Seller snapshot (for Pakka bills show GSTIN)
        SellerProfile seller = sellerRepo.findTopByOrderByIdAsc().orElse(null);
        String sellerName = seller != null ? seller.getDisplayName() : "Your Store";
        String sellerGstin = ("Pakka".equalsIgnoreCase(billType) && seller != null) ? seller.getGstin() : null;

        // 5) Invoice number
        String prefix = "P" + ( "Pakka".equalsIgnoreCase(billType) ? "K" : "C" ); // PK or PC
        String invoice = invoiceNumberService.next(prefix);

        // 6) Fill order
        order.setBillType(billType);
        order.setSubTotal(sub);
        order.setGstRate(rate);
        order.setGstAmount(gstAmt);
        order.setTotal(total);
        order.setSellerName(sellerName);
        order.setSellerGstin(sellerGstin);
        order.setInvoiceNumber(invoice);
        
        // Set tax breakdown fields
        order.setTaxType(taxType);
        order.setCgstRate(cgstRate);
        order.setCgstAmount(cgstAmount);
        order.setSgstRate(sgstRate);
        order.setSgstAmount(sgstAmount);
        order.setIgstRate(igstRate);
        order.setIgstAmount(igstAmount);
        // Keep the order status as set during order creation (Pending)
        // order.setOrderStatus("Confirmed"); // Removed - status should only be changed by admin

        orderRepository.save(order);

        // 7) Ledger entry
        ledgerRepo.save(new LedgerEntry(order.getId(), billType, total, "Sale - INV " + invoice));
        
        // 8) Create outstanding item for B2B orders (Kaccha) and apply advance payments
        // Only create outstanding for confirmed orders (not Pending or Cancelled)
        if ("Kaccha".equalsIgnoreCase(billType) && total.compareTo(BigDecimal.ZERO) > 0 && 
            !"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
            try {
                // Check if outstanding item already exists
                List<com.brsons.model.Outstanding> existingOutstanding = outstandingRepository
                    .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                
                if (existingOutstanding.isEmpty()) {
                    System.out.println("Creating outstanding item for new order ID: " + order.getId());
                    
                    // Create outstanding item
                    com.brsons.model.Outstanding outstanding = new com.brsons.model.Outstanding(
                        com.brsons.model.Outstanding.OutstandingType.INVOICE_RECEIVABLE,
                        order.getId(),
                        "ORDER",
                        invoice,
                        total,
                        LocalDateTime.now().plusDays(30),
                        order.getName(),
                        billType
                    );
                    outstanding.setDescription("Customer invoice for order #" + order.getId());
                    outstanding.setContactInfo(order.getUserPhone());
                    com.brsons.model.Outstanding savedOutstanding = outstandingRepository.save(outstanding);
                    
                    System.out.println("Created outstanding item for new order ID: " + order.getId());
                    
                    // Apply advance payments to this new invoice (FIFO)
                    try {
                        customerLedgerService.applyAdvancePaymentsToNewInvoice(order.getUserPhone(), savedOutstanding.getId(), total);
                        System.out.println("Applied advance payments to new invoice #" + savedOutstanding.getId());
                    } catch (Exception e) {
                        System.err.println("Error applying advance payments to new invoice #" + savedOutstanding.getId() + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("Outstanding item already exists for order ID: " + order.getId());
                }
            } catch (Exception e) {
                System.err.println("Error creating outstanding item for order ID " + order.getId() + ": " + e.getMessage());
            }
        }
    }
}
