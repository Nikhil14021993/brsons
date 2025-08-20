package com.brsons.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brsons.model.*;
import com.brsons.repository.*;

@Service
public class OrderAccountingService {

    private final ProductRepository productRepository; // assumes exists
    private final SellerProfileRepository sellerRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final InvoiceNumberService invoiceNumberService;
    private final OrderRepository orderRepository;

    public OrderAccountingService(
        ProductRepository productRepository,
        SellerProfileRepository sellerRepo,
        LedgerEntryRepository ledgerRepo,
        InvoiceNumberService invoiceNumberService,
        OrderRepository orderRepository
    ) {
        this.productRepository = productRepository;
        this.sellerRepo = sellerRepo;
        this.ledgerRepo = ledgerRepo;
        this.invoiceNumberService = invoiceNumberService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void finalizeTotalsAndInvoice(Order order, BigDecimal gstRatePct, String billType, String userType) {
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

        // 2) GST
        BigDecimal rate = gstRatePct == null ? BigDecimal.ZERO : gstRatePct;
        BigDecimal gstAmt = sub.multiply(rate).divide(BigDecimal.valueOf(100));

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
        order.setOrderStatus("Confirmed");

        orderRepository.save(order);

        // 7) Ledger entry
        ledgerRepo.save(new LedgerEntry(order.getId(), billType, total, "Sale - INV " + invoice));
    }
}
