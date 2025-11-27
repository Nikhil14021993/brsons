package com.brsons.service;

import com.brsons.dto.DaybookEntryDto;
import com.brsons.dto.DaybookSummaryDto;
import com.brsons.model.*;
import com.brsons.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DaybookService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherEntryRepository voucherEntryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private GRNRepository grnRepository;

    @Autowired
    private OutstandingRepository outstandingRepository;

    @Autowired
    private SupplierLedgerEntryRepository supplierLedgerEntryRepository;

    @Autowired
    private CustomerLedgerEntryRepository customerLedgerEntryRepository;

    /**
     * Get all daybook entries for a specific date range
     */
    public List<DaybookEntryDto> getDaybookEntries(LocalDate startDate, LocalDate endDate) {
        List<DaybookEntryDto> entries = new ArrayList<>();

        // Add voucher entries
        entries.addAll(getVoucherEntries(startDate, endDate));

        // Add order entries
        entries.addAll(getOrderEntries(startDate, endDate));

        // Add purchase order entries
        entries.addAll(getPurchaseOrderEntries(startDate, endDate));

        // Add GRN entries
        entries.addAll(getGRNEntries(startDate, endDate));

        // Add outstanding payment entries
        entries.addAll(getOutstandingPaymentEntries(startDate, endDate));

        // Add supplier ledger entries
        entries.addAll(getSupplierLedgerEntries(startDate, endDate));

        // Add customer ledger entries
        entries.addAll(getCustomerLedgerEntries(startDate, endDate));

        // Sort by date and time
        return entries.stream()
                .sorted((e1, e2) -> {
                    int dateCompare = e1.getDate().compareTo(e2.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return e1.getTime().compareTo(e2.getTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get voucher entries
     */
    private List<DaybookEntryDto> getVoucherEntries(LocalDate startDate, LocalDate endDate) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getDate().isAfter(startDate.minusDays(1)) && v.getDate().isBefore(endDate.plusDays(1)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (Voucher voucher : vouchers) {
            List<VoucherEntry> voucherEntries = voucherEntryRepository.findAll().stream()
                    .filter(ve -> ve.getVoucher().getId().equals(voucher.getId()))
                    .collect(Collectors.toList());
            
            for (VoucherEntry entry : voucherEntries) {
                DaybookEntryDto daybookEntry = new DaybookEntryDto();
                daybookEntry.setDate(voucher.getDate());
                daybookEntry.setTime(voucher.getDate().atStartOfDay());
                daybookEntry.setTransactionType("VOUCHER");
                daybookEntry.setTransactionId(voucher.getId().toString());
                daybookEntry.setReferenceNumber(voucher.getNarration());
                daybookEntry.setAccountName(entry.getAccount().getName());
                daybookEntry.setAccountCode(entry.getAccount().getCode());
                daybookEntry.setParticulars(entry.getDescription());
                daybookEntry.setDebitAmount(entry.getDebit());
                daybookEntry.setCreditAmount(entry.getCredit());
                daybookEntry.setVoucherType(voucher.getType());
                daybookEntry.setNarration(voucher.getNarration());
                entries.add(daybookEntry);
            }
        }
        return entries;
    }

    /**
     * Get order entries
     */
    private List<DaybookEntryDto> getOrderEntries(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate.atStartOfDay()) && 
                           o.getCreatedAt().isBefore(endDate.atTime(23, 59, 59)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (Order order : orders) {
            if ("Confirmed".equals(order.getOrderStatus()) && order.getTotal() != null) {
                DaybookEntryDto daybookEntry = new DaybookEntryDto();
                daybookEntry.setDate(order.getCreatedAt().toLocalDate());
                daybookEntry.setTime(order.getCreatedAt());
                daybookEntry.setTransactionType("ORDER");
                daybookEntry.setTransactionId(order.getId().toString());
                daybookEntry.setReferenceNumber(order.getInvoiceNumber() != null ? order.getInvoiceNumber() : "ORD-" + order.getId());
                daybookEntry.setAccountName(order.getName());
                daybookEntry.setAccountCode(order.getBillType()); // Pakka/Kaccha
                daybookEntry.setParticulars("Order - " + order.getName());
                daybookEntry.setDebitAmount(order.getTotal());
                daybookEntry.setCreditAmount(BigDecimal.ZERO);
                daybookEntry.setVoucherType("SALES");
                daybookEntry.setNarration("Order Confirmation - " + order.getName());
                entries.add(daybookEntry);
            }
        }
        return entries;
    }

    /**
     * Get purchase order entries
     */
    private List<DaybookEntryDto> getPurchaseOrderEntries(LocalDate startDate, LocalDate endDate) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getCreatedAt().isAfter(startDate.atStartOfDay()) && 
                            po.getCreatedAt().isBefore(endDate.atTime(23, 59, 59)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (PurchaseOrder po : purchaseOrders) {
            if ("APPROVED".equals(po.getStatus()) && po.getTotalAmount() != null) {
                DaybookEntryDto daybookEntry = new DaybookEntryDto();
                daybookEntry.setDate(po.getCreatedAt().toLocalDate());
                daybookEntry.setTime(po.getCreatedAt());
                daybookEntry.setTransactionType("PURCHASE_ORDER");
                daybookEntry.setTransactionId(po.getId().toString());
                daybookEntry.setReferenceNumber("PO-" + po.getId());
                daybookEntry.setAccountName(po.getSupplier().getCompanyName());
                daybookEntry.setAccountCode("SUPPLIER");
                daybookEntry.setParticulars("Purchase Order - " + po.getSupplier().getCompanyName());
                daybookEntry.setDebitAmount(BigDecimal.ZERO);
                daybookEntry.setCreditAmount(po.getTotalAmount());
                daybookEntry.setVoucherType("PURCHASE");
                daybookEntry.setNarration("Purchase Order - " + po.getSupplier().getCompanyName());
                entries.add(daybookEntry);
            }
        }
        return entries;
    }

    /**
     * Get GRN entries
     */
    private List<DaybookEntryDto> getGRNEntries(LocalDate startDate, LocalDate endDate) {
        List<GoodsReceivedNote> grns = grnRepository.findAll().stream()
                .filter(grn -> grn.getReceivedDate().isAfter(startDate.minusDays(1)) && 
                              grn.getReceivedDate().isBefore(endDate.plusDays(1)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (GoodsReceivedNote grn : grns) {
            if ("APPROVED".equals(grn.getStatus().name()) && grn.getTotalAmount() != null) {
                DaybookEntryDto daybookEntry = new DaybookEntryDto();
                daybookEntry.setDate(grn.getReceivedDate());
                daybookEntry.setTime(grn.getReceivedDate().atStartOfDay());
                daybookEntry.setTransactionType("GRN");
                daybookEntry.setTransactionId(grn.getId().toString());
                daybookEntry.setReferenceNumber(grn.getGrnNumber());
                daybookEntry.setAccountName(grn.getSupplier().getCompanyName());
                daybookEntry.setAccountCode("SUPPLIER");
                daybookEntry.setParticulars("GRN - " + grn.getSupplier().getCompanyName());
                daybookEntry.setDebitAmount(grn.getTotalAmount());
                daybookEntry.setCreditAmount(BigDecimal.ZERO);
                daybookEntry.setVoucherType("PURCHASE");
                daybookEntry.setNarration("GRN Approval - " + grn.getGrnNumber());
                entries.add(daybookEntry);
            }
        }
        return entries;
    }

    /**
     * Get outstanding payment entries
     */
    private List<DaybookEntryDto> getOutstandingPaymentEntries(LocalDate startDate, LocalDate endDate) {
        List<Outstanding> outstandingItems = outstandingRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate.atStartOfDay()) && 
                           o.getCreatedAt().isBefore(endDate.atTime(23, 59, 59)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (Outstanding outstanding : outstandingItems) {
            if ("SETTLED".equals(outstanding.getStatus().name()) && outstanding.getAmount() != null) {
                DaybookEntryDto daybookEntry = new DaybookEntryDto();
                daybookEntry.setDate(outstanding.getCreatedAt().toLocalDate());
                daybookEntry.setTime(outstanding.getCreatedAt());
                daybookEntry.setTransactionType("OUTSTANDING_PAYMENT");
                daybookEntry.setTransactionId(outstanding.getId().toString());
                daybookEntry.setReferenceNumber(outstanding.getReferenceNumber());
                daybookEntry.setAccountName(outstanding.getContactInfo());
                daybookEntry.setAccountCode("PAYMENT");
                daybookEntry.setParticulars("Payment - " + outstanding.getReferenceNumber());
                daybookEntry.setDebitAmount(BigDecimal.ZERO);
                daybookEntry.setCreditAmount(outstanding.getAmount());
                daybookEntry.setVoucherType("PAYMENT");
                daybookEntry.setNarration("Outstanding Payment - " + outstanding.getReferenceNumber());
                entries.add(daybookEntry);
            }
        }
        return entries;
    }

    /**
     * Get supplier ledger entries
     */
    private List<DaybookEntryDto> getSupplierLedgerEntries(LocalDate startDate, LocalDate endDate) {
        List<SupplierLedgerEntry> ledgerEntries = supplierLedgerEntryRepository.findAll().stream()
                .filter(se -> se.getEntryDate().isAfter(startDate.atStartOfDay()) && 
                            se.getEntryDate().isBefore(endDate.atTime(23, 59, 59)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (SupplierLedgerEntry entry : ledgerEntries) {
            DaybookEntryDto daybookEntry = new DaybookEntryDto();
            daybookEntry.setDate(entry.getEntryDate().toLocalDate());
            daybookEntry.setTime(entry.getEntryDate());
            daybookEntry.setTransactionType("SUPPLIER_LEDGER");
            daybookEntry.setTransactionId(entry.getId().toString());
            daybookEntry.setReferenceNumber(entry.getReferenceNumber());
            daybookEntry.setAccountName(entry.getSupplierLedger().getSupplierName());
            daybookEntry.setAccountCode("SUPPLIER");
            daybookEntry.setParticulars(entry.getParticulars());
            daybookEntry.setDebitAmount(entry.getDebitAmount());
            daybookEntry.setCreditAmount(entry.getCreditAmount());
            daybookEntry.setVoucherType("LEDGER");
            daybookEntry.setNarration(entry.getParticulars());
            entries.add(daybookEntry);
        }
        return entries;
    }

    /**
     * Get customer ledger entries
     */
    private List<DaybookEntryDto> getCustomerLedgerEntries(LocalDate startDate, LocalDate endDate) {
        List<CustomerLedgerEntry> ledgerEntries = customerLedgerEntryRepository.findAll().stream()
                .filter(ce -> ce.getEntryDate().isAfter(startDate.atStartOfDay()) && 
                            ce.getEntryDate().isBefore(endDate.atTime(23, 59, 59)))
                .collect(Collectors.toList());
        List<DaybookEntryDto> entries = new ArrayList<>();

        for (CustomerLedgerEntry entry : ledgerEntries) {
            DaybookEntryDto daybookEntry = new DaybookEntryDto();
            daybookEntry.setDate(entry.getEntryDate().toLocalDate());
            daybookEntry.setTime(entry.getEntryDate());
            daybookEntry.setTransactionType("CUSTOMER_LEDGER");
            daybookEntry.setTransactionId(entry.getId().toString());
            daybookEntry.setReferenceNumber(entry.getReferenceNumber());
            daybookEntry.setAccountName(entry.getCustomerLedger().getCustomerName());
            daybookEntry.setAccountCode("CUSTOMER");
            daybookEntry.setParticulars(entry.getParticulars());
            daybookEntry.setDebitAmount(entry.getDebitAmount());
            daybookEntry.setCreditAmount(entry.getCreditAmount());
            daybookEntry.setVoucherType("LEDGER");
            daybookEntry.setNarration(entry.getParticulars());
            entries.add(daybookEntry);
        }
        return entries;
    }

    /**
     * Get daybook summary for a date range
     */
    public DaybookSummaryDto getDaybookSummary(LocalDate startDate, LocalDate endDate) {
        List<DaybookEntryDto> entries = getDaybookEntries(startDate, endDate);
        
        BigDecimal totalDebits = entries.stream()
                .map(DaybookEntryDto::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalCredits = entries.stream()
                .map(DaybookEntryDto::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DaybookSummaryDto summary = new DaybookSummaryDto();
        summary.setTotalEntries(entries.size());
        summary.setTotalDebits(totalDebits);
        summary.setTotalCredits(totalCredits);
        summary.setStartDate(startDate);
        summary.setEndDate(endDate);
        
        return summary;
    }
}