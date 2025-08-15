package com.brsons.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.brsons.model.AddToCart;
import com.brsons.model.CartItemDetails;
import com.brsons.model.CartProductEntry;
import com.brsons.model.CartProductEntry1;
import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.User;
import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CartProductEntryRepo;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.service.CheckoutService;
import com.brsons.service.InvoicePdfService;
import com.brsons.service.OrderAccountingService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class CheckoutController {

	@Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    OrderAccountingService orderAccountingService;
    @Autowired
    InvoicePdfService invoiceService;

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model, HttpServletResponse response) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
   
        model.addAttribute("userPhone", user.getPhone());
        return "checkout";
    }
    
    @Autowired
    private CartProductEntryRepo cartRepo;

    

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String name,
                                  @RequestParam String addressLine1,
                                  @RequestParam String addressLine2,
                                  @RequestParam String city,
                                  @RequestParam String state,
                                  @RequestParam String zipCode,
                                  @RequestParam(required = false) String billType,
                                  @RequestParam(required = false) String buyerGstin,
                                  HttpSession session,  HttpServletResponse response) throws IOException {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // ✅ Fetch cart from DB using phone number
        List<CartProductEntry1> cartItems = cartRepo.findByUserPhone(user.getPhone());
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        // ✅ Create Order
        Order order = new Order();
        order.setName(name);
        order.setUserPhone(user.getPhone());
        order.setAddressLine1(addressLine1);
        order.setAddressLine2(addressLine2);
        order.setCity(city);
        order.setState(state);
        order.setZipCode(zipCode);
        order.setStatus("Active");
        order.setOrderStatus("Not Confirmed");
        
        

        // ✅ Create Order Items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartProductEntry1 cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setProductId(cartItem.getProductId());
            item.setQuantity(cartItem.getQuantity());
            item.setOrder(order);
            orderItems.add(item);
        }
       
        order.setOrderItems(orderItems);
        order.setBillType(billType != null ? billType : "Pakka");
        order.setBuyerGstin(buyerGstin);
        // ✅ Save order with items
        orderRepository.save(order);
        
     // ✅ Finalize GST + invoice + ledger
        orderAccountingService.finalizeTotalsAndInvoice(order, new BigDecimal("5.00"),  order.getBillType());

        // ✅ Mark cart items checked_out = 'yes' (you already added this)
        //checkoutService.markCheckedOut(user.getPhone());

        // ✅ Optionally clear cart from DB
       // cartRepo.deleteAll(cartItems);
        // ✅ Generate invoice PDF and send as response
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice.pdf");
        invoiceService.generateInvoicePdf(user.getPhone(), response.getOutputStream());
        checkoutService.clearCart(user.getPhone());
        return "redirect:/";
    }

}

