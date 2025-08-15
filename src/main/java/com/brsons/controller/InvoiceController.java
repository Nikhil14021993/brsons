package com.brsons.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;
import com.brsons.service.InvoicePdfService;

@Controller
public class InvoiceController {

    private final OrderRepository orderRepository;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(OrderRepository orderRepository, InvoicePdfService invoicePdfService) {
        this.orderRepository = orderRepository;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/orders/{id}/invoice.pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        byte[] pdf = invoicePdfService.render( order);
        
        String filename = (order.getInvoiceNumber() != null ? order.getInvoiceNumber() : ("INV-" + order.getId())) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
