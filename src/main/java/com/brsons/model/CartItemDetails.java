package com.brsons.model;

public class CartItemDetails {
    private Product product;
    private int quantity;
    private double totalPrice;

    public CartItemDetails(Product product, int quantity, double totalPrice) {
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}

