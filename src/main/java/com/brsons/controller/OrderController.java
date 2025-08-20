package com.brsons.controller;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.brsons.model.User;
import com.brsons.repository.InvoiceRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.model.Invoice;
import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import com.brsons.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// PDF Generation imports
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.lowagie.text.Chunk;
import java.awt.Color;

@Controller
public class OrderController {
	
	 @PersistenceContext
	    private EntityManager entityManager;
@Autowired
OrderService orderService;

@Autowired
private ProductRepository productRepository;

@Autowired private InvoiceRepository invoiceRepository;
@Autowired private OrderRepository orderRepository;

@Value("${invoice.storage.dir:/opt/brsons/invoices}")
private String invoiceStorageDir;
	
	@GetMapping("/orders")
	public String viewOrders(HttpSession session, Model model) {
	    try {
	        User user = (User) session.getAttribute("user");
	        
	        if (user == null) {
	            return "redirect:/login";
	        }
	        
	        List<com.brsons.model.Order> orders = orderService.getOrdersByUserPhone(user.getPhone());
	        model.addAttribute("orders", orders);
	        return "orders";
	    } catch (Exception e) {
	        System.err.println("Error in viewOrders: " + e.getMessage());
	        e.printStackTrace();
	        model.addAttribute("error", "Error loading orders: " + e.getMessage());
	        return "orders";
	    }
	}
	
	@GetMapping("/orders/{orderId}")
	public String getOrderDetails(@PathVariable Long orderId, Model model) {
	    // Fetch order item details (photo, quantity, unit_price, total_price) from OrderItem
		List<Object[]> items = entityManager.createQuery(
		        "SELECT p.id, p.mainPhoto, i.quantity, i.unitPrice, i.totalPrice " +
		        "FROM OrderItem i JOIN Product p ON i.productId = p.id " +
		        "WHERE i.order.id = :orderId", Object[].class)
		        .setParameter("orderId", orderId)
		        .getResultList();

	    model.addAttribute("items", items);

	    // Calculate subtotal from stored total prices
	    Double subtotal = items.stream()
	            .mapToDouble(r -> {
	                if (r[4] != null) {
	                    // Use stored total price if available
	                    return ((Number) r[4]).doubleValue();
	                } else if (r[3] != null) {
	                    // Fallback: calculate from unit price and quantity
	                    return ((Number) r[3]).doubleValue() * ((Number) r[2]).doubleValue();
	                }
	                return 0.0;
	            })
	            .sum();

	    model.addAttribute("subtotal", subtotal);
	    model.addAttribute("orderId", orderId);

	    return "order-details"; // Thymeleaf page
	}
	
	@PostMapping("/orders/{orderId}/cancel")
	public ResponseEntity<String> cancelOrder(@PathVariable Long orderId, HttpSession session) {
	    try {
	        User user = (User) session.getAttribute("user");
	        if (user == null) {
	            return ResponseEntity.status(401).body("User not authenticated");
	        }
	        
	        // Get order details
	        Order order = orderService.getOrderById(orderId);
	        if (order == null || !order.getUserPhone().equals(user.getPhone())) {
	            return ResponseEntity.status(404).body("Order not found");
	        }
	        
	        // Check if order can be cancelled (within 14 days and not delivered/cancelled)
	        if (order.getOrderStatus() != null && 
	            (order.getOrderStatus().equals("Delivered") || order.getOrderStatus().equals("Cancelled"))) {
	            return ResponseEntity.badRequest().body("Order cannot be cancelled");
	        }
	        
	        // Update order status to cancelled
	        order.setOrderStatus("Cancelled");
	        orderService.updateOrder(order);
	        
	        return ResponseEntity.ok("Order cancelled successfully");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).body("Error cancelling order: " + e.getMessage());
	    }
	}
	@GetMapping("/invoices/{orderId}/download")
	public void downloadInvoice(@PathVariable Long orderId, HttpServletResponse response) throws IOException {
	    Invoice invoice = invoiceRepository.findByOrder_Id(orderId)
	        .orElseThrow(() -> new RuntimeException("Invoice not found"));

	    Path filePath = Paths.get(invoice.getFilePath());
	    if (!Files.exists(filePath)) {
	        throw new RuntimeException("File not found");
	    }

	    response.setContentType("application/pdf");
	    response.setHeader("Content-Disposition", "inline; filename=" + invoice.getFileName());
	    Files.copy(filePath, response.getOutputStream());
	}
	@GetMapping("/orders/{orderId}/invoice")
	public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable Long orderId, HttpSession session) {
	    try {
	        User user = (User) session.getAttribute("user");
	        if (user == null) return ResponseEntity.status(401).build();

	        Order order = orderService.getOrderById(orderId);
	        if (order == null || !order.getUserPhone().equals(user.getPhone())) {
	            return ResponseEntity.status(404).build();
	        }

	        // 1) If we already have an invoice record & file, stream it
	        Optional<Invoice> existing = invoiceRepository.findByOrder_Id(orderId);
	        if (existing.isPresent()) {
	            Path path = Paths.get(existing.get().getFilePath());
	            if (Files.exists(path)) {
	                byte[] bytes = Files.readAllBytes(path);
	                HttpHeaders headers = new HttpHeaders();
	                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + existing.get().getFileName());
	                return ResponseEntity.ok()
	                        .headers(headers)
	                        .contentType(MediaType.APPLICATION_PDF)
	                        .body(new ByteArrayResource(bytes));
	            }
	            // falls through to regenerate if file missing
	        }

	        // 2) Otherwise, generate now, save to disk & DB, and stream
	        byte[] pdfContent = generatePdfInvoice(order); // your existing generator
	        Invoice saved = savePdfToDiskAndDb(order, pdfContent);

	        HttpHeaders headers = new HttpHeaders();
	        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + saved.getFileName());
	        return ResponseEntity.ok()
	                .headers(headers)
	                .contentType(MediaType.APPLICATION_PDF)
	                .body(new ByteArrayResource(pdfContent));

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).build();
	    }
	}

    private String formatDateSafe(Object dateObj, SimpleDateFormat sdf) {
        if (dateObj == null) return "";
        if (dateObj instanceof Date) {
            return sdf.format((Date) dateObj);
        } else if (dateObj instanceof LocalDateTime) {
            Date date = Date.from(((LocalDateTime) dateObj).atZone(ZoneId.systemDefault()).toInstant());
            return sdf.format(date);
        } else if (dateObj instanceof String) {
            return dateObj.toString(); // Already string, just return
        } else {
            return "";
        }
    }
	private byte[] generatePdfInvoice(Order order) throws DocumentException, IOException {
	    Document document = new Document(PageSize.A4);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PdfWriter writer = PdfWriter.getInstance(document, baos);
	    
	    document.open();
	    
	    // Set up fonts
	    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLUE);
	    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
	    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
	    Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
	    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.RED);
	    
	    
	    Paragraph seller = new Paragraph("BRSONS Cloth Store\nGST: 27XXXXX1234Z5\n123 Main Street, Mumbai\nPhone: +91-9999999999",
                new Font(Font.HELVETICA, 10, Font.NORMAL));
        seller.setAlignment(Element.ALIGN_LEFT);
        document.add(seller);

        document.add(new Paragraph("\n"));
        
     // 4️⃣ Buyer Info
        Paragraph buyer = new Paragraph("Invoice To:\n" + order.getName() + "\n" +
                order.getAddressLine1() + ", " + order.getAddressLine2() + "\n" +
                order.getCity() + ", " + order.getState() + " - " + order.getZipCode() +
                "\nGSTIN: " + (order.getBuyerGstin() != null ? order.getBuyerGstin() : "N/A"),
                new Font(Font.HELVETICA, 10, Font.NORMAL));
        buyer.setAlignment(Element.ALIGN_LEFT);
        document.add(buyer);

        document.add(new Paragraph("\n"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String formattedDate = formatDateSafe(order.getCreatedAt(), sdf);

        // Invoice metadata
        Paragraph invoiceMeta = new Paragraph(
                "Invoice No: " + order.getInvoiceNumber() + "\n" +
                        "Date: " + formattedDate + "\n" +
                        "Bill Type: " + order.getBillType(),
                new Font(Font.HELVETICA, 10, Font.NORMAL));
        invoiceMeta.setAlignment(Element.ALIGN_LEFT);
        document.add(invoiceMeta);
        document.add(new Paragraph("\n"));
	    
		    // =====================
	    // Product Details Table
	    // =====================
	    Paragraph productHeader = new Paragraph("Product Details", headerFont);
	    productHeader.setSpacingBefore(20);
	    productHeader.setSpacingAfter(10);
	    document.add(productHeader);

	    PdfPTable productTable = new PdfPTable(5); // 5 columns: Sr, Name, Price, Qty, Total
	    productTable.setWidthPercentage(100);
	    productTable.setSpacingBefore(10);
	    productTable.setSpacingAfter(20);
	    productTable.setWidths(new float[]{1f, 4f, 2f, 1.5f, 2f});

	    // Table header row
	    addTableHeader(productTable, new String[]{"Sr No", "Product Name", "Price", "Quantity", "Total"}, headerFont);

	    int srNo = 1;
	    for (OrderItem item : order.getOrderItems()) { // assuming Order has getItems()
	        productTable.addCell(new PdfPCell(new Phrase(String.valueOf(srNo++), normalFont)));
	        Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found for ID: " + item.getProductId()));
	        productTable.addCell(new PdfPCell(new Phrase(product.getProductName(), normalFont)));
	        
	                // Use the stored unit price from OrderItem
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        productTable.addCell(new PdfPCell(new Phrase("₹" + unitPrice, normalFont)));
        productTable.addCell(new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont)));
        // Use the stored total price from OrderItem
        BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
        productTable.addCell(new PdfPCell(new Phrase("₹" + itemTotal, normalFont)));
	    }

	   // document.add(productTable);

	    
	    
	    
	    PdfPCell emptyCell = new PdfPCell(new Phrase(""));
	    emptyCell.setColspan(3);
	    emptyCell.setBorder(Rectangle.NO_BORDER);

	    // Sub Total
	    productTable.addCell(emptyCell);
	    productTable.addCell(new PdfPCell(new Phrase("Sub Total:", headerFont)));
	    productTable.addCell(new PdfPCell(new Phrase("₹" + (order.getSubTotal() != null ? order.getSubTotal().toString() : "0.00"), normalFont)));

	    // GST Rate
	    productTable.addCell(emptyCell);
	    productTable.addCell(new PdfPCell(new Phrase("GST Rate:", headerFont)));
	    productTable.addCell(new PdfPCell(new Phrase((order.getGstRate() != null ? order.getGstRate().toString() : "0") + "%", normalFont)));

	    // GST Amount
	    productTable.addCell(emptyCell);
	    productTable.addCell(new PdfPCell(new Phrase("GST Amount:", headerFont)));
	    productTable.addCell(new PdfPCell(new Phrase("₹" + (order.getGstAmount() != null ? order.getGstAmount().toString() : "0.00"), normalFont)));

	    // Total Amount (bold & highlighted)
	    
	    productTable.addCell(emptyCell);
	    productTable.addCell(new PdfPCell(new Phrase("TOTAL AMOUNT:", headerFont)));
	    productTable.addCell(new PdfPCell(new Phrase("₹" + (order.getTotal() != null ? order.getTotal().toString() : "0.00"), normalFont)));


	    // Finally add productTable to document
	    document.add(productTable);
	       
	    
	 // 7️⃣ Footer
        document.add(new Paragraph("\nThank you for your purchase!", new Font(Font.HELVETICA, 10, Font.ITALIC)));
        document.add(new Paragraph("\nFor any queries, please contact us at: support@brsons.com", new Font(Font.HELVETICA, 10, Font.ITALIC)));
        document.close();
	    
	  
	    return baos.toByteArray();
	}
	
	private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
	    PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
	    labelCell.setBorderWidth(0);
	    labelCell.setPaddingBottom(5);
	    labelCell.setPaddingRight(10);
	    
	    PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
	    valueCell.setBorderWidth(0);
	    valueCell.setPaddingBottom(5);
	    valueCell.setPaddingLeft(10);
	    
	    table.addCell(labelCell);
	    table.addCell(valueCell);
	}
	
	// helper for table header
	private void addTableHeader(PdfPTable table, String[] headers, Font font) {
	    for (String header : headers) {
	        PdfPCell cell = new PdfPCell(new Phrase(header, font));
	        cell.setBackgroundColor(Color.LIGHT_GRAY);
	        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
	        cell.setPadding(5);
	        table.addCell(cell);
	    }
	
	
}
	
	private void ensureDirExists(Path dir) throws IOException {
	    if (!Files.exists(dir)) Files.createDirectories(dir);
	}

	private String ensureInvoiceNumber(Order order) {
	    if (order.getInvoiceNumber() == null || order.getInvoiceNumber().isBlank()) {
	        // Fallback format; if your finalize service already sets it, this won't run
	        String inv = "INV-" + LocalDate.now().getYear() + "-" + String.format("%06d", order.getId());
	        order.setInvoiceNumber(inv);
	        orderRepository.save(order);
	    }
	    return order.getInvoiceNumber();
	}

	private Invoice savePdfToDiskAndDb(Order order, byte[] pdfBytes) throws IOException {
	    String invoiceNumber = ensureInvoiceNumber(order);
	    String fileName = invoiceNumber + ".pdf";

	    Path dir = Paths.get(invoiceStorageDir);
	    ensureDirExists(dir);

	    Path filePath = dir.resolve(fileName);
	    Files.write(filePath, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

	    Invoice invoice = invoiceRepository.findByOrder_Id(order.getId()).orElse(new Invoice());
	    invoice.setOrder(order);
	    invoice.setInvoiceNumber(invoiceNumber);
	    invoice.setFileName(fileName);
	    invoice.setFilePath(filePath.toString());
	    return invoiceRepository.save(invoice);
	}

}
