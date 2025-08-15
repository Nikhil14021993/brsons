package com.brsons.service;

import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.ProductRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class InvoicePdfService {
	@Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    
    private final OrderItemRepository orderItemRepository;
    
    public InvoicePdfService(ProductRepository productRepository,
                                   OrderItemRepository orderItemRepository) {
                                    this.productRepository = productRepository;
                                    this.orderItemRepository = orderItemRepository;
                                    }

    public void generateInvoicePdf(String userPhone, OutputStream outputStream) {
        try {
            // 1️⃣ Fetch latest order for this user
            Order order = orderRepository.findTopByUserPhoneOrderByIdDesc(userPhone)
                    .orElseThrow(() -> new RuntimeException("No order found for user"));

            // 2️⃣ PDF Setup
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // 3️⃣ Seller Info
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

            // 5️⃣ Invoice Metadata
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String formattedDate = formatDateSafe(order.getCreatedAt(), sdf);

            // Invoice metadata
            Paragraph invoiceMeta = new Paragraph(
                    "Invoice No: INV-" + order.getId() + "\n" +
                            "Date: " + formattedDate + "\n" +
                            "Bill Type: " + order.getBillType(),
                    new Font(Font.HELVETICA, 10, Font.NORMAL));
            invoiceMeta.setAlignment(Element.ALIGN_LEFT);
            document.add(invoiceMeta);
            document.add(new Paragraph("\n"));

            // 6️⃣ Products Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{5, 40, 15, 15, 15});
            table.setSpacingBefore(10f);

            // Table Header
            table.addCell(head("S.No"));
            table.addCell(head("Product Name"));
            table.addCell(head("Qty"));
            table.addCell(head("Price"));
            table.addCell(head("Total"));

            // Table Data
            List<OrderItem> items = order.getOrderItems();
            int count = 1;
            BigDecimal subTotal = BigDecimal.ZERO;
            for (OrderItem item : items) {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found for ID: " + item.getProductId()));
                table.addCell(body(String.valueOf(count)));
                table.addCell(body(product.getProductName())); // Product Name
                table.addCell(body(String.valueOf(item.getQuantity()))); // Quantity

                // Convert price to BigDecimal
                BigDecimal price = BigDecimal.valueOf(product.getPrice());

                table.addCell(body(price.toString())); // Unit Price

                // Calculate total price = unit price * quantity
                BigDecimal total = price.multiply(BigDecimal.valueOf(item.getQuantity()));
                table.addCell(body(total.toString())); // Total Price
                subTotal = subTotal.add(total);
                count++;
            }


            // GST & Total
            BigDecimal gstAmount = subTotal.multiply(new BigDecimal("0.05")); // 5% GST
            BigDecimal grandTotal = subTotal.add(gstAmount);

            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setColspan(3);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(emptyCell);
            table.addCell(head("Subtotal"));
            table.addCell(body(subTotal.toString()));

            table.addCell(emptyCell);
            table.addCell(head("GST (5%)"));
            table.addCell(body(gstAmount.toString()));

            table.addCell(emptyCell);
            table.addCell(head("Grand Total"));
            table.addCell(body(grandTotal.toString()));

            document.add(table);

            // 7️⃣ Footer
            document.add(new Paragraph("\nThank you for your purchase!", new Font(Font.HELVETICA, 10, Font.ITALIC)));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }
    // Safe date formatting for multiple types
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
    
    
    public byte[] render(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
//            document.add(new Paragraph("Invoice #" + order.getId(), new Font(Font.HELVETICA, 18, Font.BOLD)));
            document.add(new Paragraph(" ")); // Empty line
            
         // 3️⃣ Seller Info
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

            // 5️⃣ Invoice Metadata
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String formattedDate = formatDateSafe(order.getCreatedAt(), sdf);

            // Invoice metadata
            Paragraph invoiceMeta = new Paragraph(
                    "Invoice No: INV-" + order.getId() + "\n" +
                            "Date: " + formattedDate + "\n" +
                            "Bill Type: " + order.getBillType(),
                    new Font(Font.HELVETICA, 10, Font.NORMAL));
            invoiceMeta.setAlignment(Element.ALIGN_LEFT);
            document.add(invoiceMeta);
            document.add(new Paragraph("\n"));


            // Table setup
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 1, 2, 2});

            // Table headers
            table.addCell(head("Product"));
            table.addCell(head("Qty"));
            table.addCell(head("Unit Price"));
            table.addCell(head("Total"));

            // Fetch order items from DB
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

            BigDecimal subTotal = BigDecimal.ZERO;

            for (OrderItem item : orderItems) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

                // Unit price as BigDecimal
                BigDecimal price = BigDecimal.valueOf(product.getPrice());

                // Total = price * qty
                BigDecimal total = price.multiply(BigDecimal.valueOf(item.getQuantity()));

                // Add cells
                table.addCell(body(product.getProductName()));
                table.addCell(body(String.valueOf(item.getQuantity())));
                table.addCell(body(price.toString()));
                table.addCell(body(total.toString()));

                subTotal = subTotal.add(total);
            }
            
         // GST & Total
            BigDecimal gstAmount = subTotal.multiply(new BigDecimal("0.05")); // 5% GST
            BigDecimal grandTotal = subTotal.add(gstAmount);

            // Grand total row
            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setColspan(3);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            
            table.addCell(emptyCell);
            table.addCell(body("SubTotal : " +subTotal.toString()));
            
            table.addCell(emptyCell);
            table.addCell(body("GST (5%) : " +gstAmount.toString()));
            
            table.addCell(emptyCell);              
            table.addCell(body("Grand Total : " + grandTotal));
            
            
            
            

            document.add(table);
            document.add(new Paragraph("\nThank you for your purchase!", new Font(Font.HELVETICA, 10, Font.ITALIC)));
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        }
    }

   

    private PdfPCell head(String s) {
        Font f = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell c = new PdfPCell(new Phrase(s, f));
        c.setPadding(6f);
        c.setBackgroundColor(Color.LIGHT_GRAY);
        return c;
    }

    private PdfPCell body(String s) {
        Font f = new Font(Font.HELVETICA, 9, Font.NORMAL);
        PdfPCell c = new PdfPCell(new Phrase(s, f));
        c.setPadding(5f);
        return c;
    }
}
