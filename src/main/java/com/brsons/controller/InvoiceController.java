package com.brsons.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;
// import com.brsons.service.EnhancedInvoiceService;

@Controller
public class InvoiceController {

    private final OrderRepository orderRepository;
    // private final EnhancedInvoiceService enhancedInvoiceService;

    public InvoiceController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        // this.enhancedInvoiceService = enhancedInvoiceService;
    }

    @GetMapping("/orders/{id}/invoice.pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Temporarily use old PDF generation method
        // TODO: Re-enable enhanced service after database schema is updated
        byte[] pdf = generateBasicInvoicePdf(order);
        
        String filename = (order.getInvoiceNumber() != null ? order.getInvoiceNumber() : ("INV-" + order.getId())) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    
    /**
     * Basic PDF generation method (temporary replacement)
     */
    private byte[] generateBasicInvoicePdf(Order order) {
        try {
            // Simple PDF generation for now
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add basic content
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("INVOICE", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);
            
            com.lowagie.text.Paragraph orderInfo = new com.lowagie.text.Paragraph(
                "Order ID: " + order.getId() + "\n" +
                "Customer: " + order.getName() + "\n" +
                "Total: ₹" + (order.getTotal() != null ? order.getTotal().toString() : "0.00")
            );
            document.add(orderInfo);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating basic invoice PDF: " + e.getMessage(), e);
        }
    }
}
