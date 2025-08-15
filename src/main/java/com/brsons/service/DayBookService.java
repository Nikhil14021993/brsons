package com.brsons.service;

import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DayBookService {

    private final OrderRepository orderRepository;

    public DayBookService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getDayBook(LocalDate date) {
        List<Order> orders = orderRepository.findOrdersByDate(date);

        if (orders.isEmpty()) {
            return Map.of("message", "No transactions found for this date");
        }

        double totalAmount = orders.stream()
                .map(o -> o.getTotal() != null ? o.getTotal().doubleValue() : 0.0)
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalGst = orders.stream()
                .map(o -> o.getGstAmount() != null ? o.getGstAmount().doubleValue() : 0.0)
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("orderCount", orders.size());
        result.put("totalAmount", totalAmount);
        result.put("totalGst", totalGst);
        result.put("orders", orders);

        return result;
    }
}
