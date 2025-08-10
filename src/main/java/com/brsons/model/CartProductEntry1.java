package com.brsons.model;

import jakarta.persistence.*;

@Entity
@Table(name = "add_to_cart_product_quantities", schema = "public")
public class CartProductEntry1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // primary key

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "user_phone")
    private String userPhone;

    public CartProductEntry1() {}

    public CartProductEntry1(Long productId, Integer quantity, String userPhone) {
        this.productId = productId;
        this.quantity = quantity;
        this.userPhone = userPhone;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public String getUserPhone() {
		return userPhone;
	}

	public void setUserPhone(String userPhone) {
		this.userPhone = userPhone;
	}

    // getters & setters...
}
