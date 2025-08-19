package com.brsons.service;

import com.brsons.model.Invoice;
import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import com.brsons.repository.InvoiceRepository;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.ProductRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class EnhancedInvoiceService {
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    /**
     * Generate invoice PDF at checkout time and store it in the order
     * This should be called when the order is created
     */
    public byte[] generateInvoiceAtCheckout(Order order) {
        // Generate the invoice PDF
        byte[] pdfContent = generateEnhancedInvoicePdf(order);
        
        // Store the PDF content directly in the order
        order.setInvoicePdfContent(pdfContent);
        order.setInvoiceGeneratedAt(LocalDateTime.now());
        
        return pdfContent;
    }
    
    /**
     * Get stored invoice PDF from order (no regeneration)
     * This is called when user clicks download invoice
     */
    public byte[] getStoredInvoice(Order order) {
        if (order.getInvoicePdfContent() != null && order.getInvoicePdfContent().length > 0) {
            return order.getInvoicePdfContent();
        }
        
        // If no stored invoice, generate one (fallback for existing orders)
        return generateInvoiceAtCheckout(order);
    }
    
    /**
     * Get or generate invoice PDF for an order (legacy method - kept for compatibility)
     * If invoice exists and is not expired, return cached version
     * Otherwise, generate new invoice and cache it
     */
    public byte[] getOrGenerateInvoice(Order order) {
        // Check if we have a cached, non-expired invoice
        Optional<Invoice> cachedInvoice = invoiceRepository.findByOrderIdAndNotExpired(
            order.getId(), LocalDateTime.now());
        
        if (cachedInvoice.isPresent()) {
            return cachedInvoice.get().getPdfContent();
        }
        
        // Generate new invoice
        byte[] pdfContent = generateEnhancedInvoicePdf(order);
        
        // Cache the invoice
        String invoiceNumber = order.getInvoiceNumber() != null ? 
            order.getInvoiceNumber() : "INV-" + order.getId();
        
        Invoice invoice = new Invoice(order.getId(), invoiceNumber, pdfContent);
        invoiceRepository.save(invoice);
        
        return pdfContent;
    }
    
    /**
     * Generate enhanced invoice PDF with proper table structure
     */
    private byte[] generateEnhancedInvoicePdf(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Header with company logo and info
            addHeader(document, order);
            
            // Invoice details section
            addInvoiceDetails(document, order);
            
            // Products table with proper structure
            addProductsTable(document, order);
            
            // Summary section (Subtotal, GST, Grand Total)
            addSummarySection(document, order);
            
            // Footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating enhanced invoice PDF: " + e.getMessage(), e);
        }
    }
    
    private void addHeader(Document document, Order order) throws DocumentException {
        // Company header
        Font companyFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph companyHeader = new Paragraph("BRSONS CLOTH STORE", companyFont);
        companyHeader.setAlignment(Element.ALIGN_CENTER);
        companyHeader.setSpacingAfter(10);
        document.add(companyHeader);
        
        // Company details
        Font companyDetailsFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Paragraph companyDetails = new Paragraph(
            "GST: 27XXXXX1234Z5\n" +
            "123 Main Street, Mumbai, Maharashtra - 400001\n" +
            "Phone: +91-9999999999 | Email: info@brsons.com",
            companyDetailsFont);
        companyDetails.setAlignment(Element.ALIGN_CENTER);
        companyDetails.setSpacingAfter(20);
        document.add(companyDetails);
        
        // Separator line
        LineSeparator separator = new LineSeparator();
        separator.setLineWidth(1);
        separator.setLineColor(Color.GRAY);
        document.add(separator);
        document.add(new Paragraph(" "));
    }
    
    private void addInvoiceDetails(Document document, Order order) throws DocumentException {
        // Create a table for invoice details
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{1, 1});
        
        // Left side - Invoice info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);
        
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        
        String invoiceNumber = order.getInvoiceNumber() != null ? 
            order.getInvoiceNumber() : "INV-" + order.getId();
        
        leftCell.addElement(new Paragraph("Invoice Number:", labelFont));
        leftCell.addElement(new Paragraph(invoiceNumber, valueFont));
        leftCell.addElement(new Paragraph(" "));
        
        leftCell.addElement(new Paragraph("Invoice Date:", labelFont));
        leftCell.addElement(new Paragraph(formatDate(order.getCreatedAt()), valueFont));
        leftCell.addElement(new Paragraph(" "));
        
        leftCell.addElement(new Paragraph("Bill Type:", labelFont));
        leftCell.addElement(new Paragraph(order.getBillType() != null ? order.getBillType() : "N/A", valueFont));
        
        // Right side - Customer info
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        
        rightCell.addElement(new Paragraph("Bill To:", labelFont));
        rightCell.addElement(new Paragraph(order.getName(), valueFont));
        rightCell.addElement(new Paragraph(order.getAddressLine1(), valueFont));
        if (order.getAddressLine2() != null && !order.getAddressLine2().trim().isEmpty()) {
            rightCell.addElement(new Paragraph(order.getAddressLine2(), valueFont));
        }
        rightCell.addElement(new Paragraph(order.getCity() + ", " + order.getState() + " - " + order.getZipCode(), valueFont));
        rightCell.addElement(new Paragraph(" "));
        
        if (order.getBuyerGstin() != null && !order.getBuyerGstin().trim().isEmpty()) {
            rightCell.addElement(new Paragraph("GSTIN:", labelFont));
            rightCell.addElement(new Paragraph(order.getBuyerGstin(), valueFont));
        }
        
        detailsTable.addCell(leftCell);
        detailsTable.addCell(rightCell);
        
        document.add(detailsTable);
        document.add(new Paragraph(" "));
    }
    
    private void addProductsTable(Document document, Order order) throws DocumentException {
        // Create products table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.8f, 3.5f, 1.0f, 1.2f, 1.5f});
        table.setSpacingBefore(15);
        table.setSpacingAfter(15);
        
        // Table headers
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Color headerColor = new Color(70, 130, 180); // Steel blue
        
        addTableHeader(table, "S.No", headerFont, headerColor);
        addTableHeader(table, "Product Name", headerFont, headerColor);
        addTableHeader(table, "Quantity", headerFont, headerColor);
        addTableHeader(table, "Unit Price", headerFont, headerColor);
        addTableHeader(table, "Total", headerFont, headerColor);
        
        // Get order items from the order
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            // Fallback: try to fetch from repository
            orderItems = orderItemRepository.findByOrder(order);
        }
        
        // Table data
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        BigDecimal subTotal = BigDecimal.ZERO;
        
        if (orderItems != null && !orderItems.isEmpty()) {
            for (int i = 0; i < orderItems.size(); i++) {
                OrderItem item = orderItems.get(i);
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
                
                // S.No
                addTableCell(table, String.valueOf(i + 1), dataFont, Element.ALIGN_CENTER);
                
                // Product Name
                addTableCell(table, product.getProductName(), dataFont, Element.ALIGN_LEFT);
                
                // Quantity
                addTableCell(table, String.valueOf(item.getQuantity()), dataFont, Element.ALIGN_CENTER);
                
                // Unit Price
                BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
                addTableCell(table, "₹" + unitPrice.toString(), dataFont, Element.ALIGN_RIGHT);
                
                // Total
                BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                addTableCell(table, "₹" + total.toString(), dataFont, Element.ALIGN_RIGHT);
                
                subTotal = subTotal.add(total);
            }
        } else {
            // If no order items, show a placeholder row
            addTableCell(table, "1", dataFont, Element.ALIGN_CENTER);
            addTableCell(table, "Product Details Not Available", dataFont, Element.ALIGN_LEFT);
            addTableCell(table, "1", dataFont, Element.ALIGN_CENTER);
            addTableCell(table, "₹" + (order.getTotal() != null ? order.getTotal().toString() : "0"), dataFont, Element.ALIGN_RIGHT);
            addTableCell(table, "₹" + (order.getTotal() != null ? order.getTotal().toString() : "0"), dataFont, Element.ALIGN_RIGHT);
            subTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        }
        
        document.add(table);
        
        // Store subtotal for summary section
        document.add(new Paragraph(" "));
    }
    
    private void addSummarySection(Document document, Order order) throws DocumentException {
        // Use the order's stored values if available, otherwise calculate
        BigDecimal subTotal = order.getSubTotal();
        BigDecimal gstRate = order.getGstRate();
        BigDecimal gstAmount = order.getGstAmount();
        BigDecimal grandTotal = order.getTotal();
        
        // If order doesn't have these values, use defaults
        if (subTotal == null) {
            subTotal = BigDecimal.ZERO;
        }
        if (gstRate == null) {
            gstRate = new BigDecimal("5.00");
        }
        if (gstAmount == null) {
            gstAmount = subTotal.multiply(gstRate).divide(new BigDecimal("100"));
        }
        if (grandTotal == null) {
            grandTotal = subTotal.add(gstAmount);
        }
        
        // Create summary table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setWidths(new float[]{1, 1});
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setSpacingBefore(10);
        
        Font summaryLabelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font summaryValueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        
        // Subtotal row
        addSummaryRow(summaryTable, "Subtotal:", "₹" + subTotal.toString(), summaryLabelFont, summaryValueFont);
        
        // GST row
        addSummaryRow(summaryTable, "GST (" + gstRate + "%):", "₹" + gstAmount.toString(), summaryLabelFont, summaryValueFont);
        
        // Separator line
        addSummaryRow(summaryTable, "", "", summaryLabelFont, summaryValueFont);
        
        // Grand Total row
        Font grandTotalFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        addSummaryRow(summaryTable, "Grand Total:", "₹" + grandTotal.toString(), grandTotalFont, grandTotalFont);
        
        document.add(summaryTable);
    }
    
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        
        // Thank you message
        Font thankYouFont = new Font(Font.HELVETICA, 10, Font.ITALIC);
        Paragraph thankYou = new Paragraph("Thank you for your purchase!", thankYouFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(20);
        document.add(thankYou);
        
        // Terms and conditions
        Font termsFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Paragraph terms = new Paragraph(
            "Terms & Conditions:\n" +
            "• Goods once sold will not be taken back or exchanged\n" +
            "• Payment should be made at the time of delivery\n" +
            "• Subject to local jurisdiction only",
            termsFont);
        terms.setAlignment(Element.ALIGN_CENTER);
        terms.setSpacingBefore(15);
        document.add(terms);
    }
    
    private void addTableHeader(PdfPTable table, String text, Font font, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(color);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(Color.WHITE);
        table.addCell(cell);
    }
    
    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }
    
    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private String formatDate(Object dateObj) {
        if (dateObj == null) return "N/A";
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        if (dateObj instanceof Date) {
            return sdf.format((Date) dateObj);
        } else if (dateObj instanceof LocalDateTime) {
            Date date = Date.from(((LocalDateTime) dateObj).atZone(ZoneId.systemDefault()).toInstant());
            return sdf.format(date);
        } else {
            return dateObj.toString();
        }
    }
    
    /**
     * Clean up expired invoices
     */
    public void cleanupExpiredInvoices() {
        List<Invoice> expiredInvoices = invoiceRepository.findExpiredInvoices(LocalDateTime.now());
        invoiceRepository.deleteAll(expiredInvoices);
    }
    

}
