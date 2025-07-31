package com.brsons.model;

public class Product {
	private int id;
    private String name;
    private double price;
    private String imageUrl;
	public int getId() {
		return id;
	}
	public Product(int id, String name, double price, String imageUrl) {
		super();
		this.id = id;
		this.name = name;
		this.price = price;
		this.imageUrl = imageUrl;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
