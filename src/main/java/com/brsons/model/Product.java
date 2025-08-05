package com.brsons.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private String colour;
    private Double price;
    private String size;
    private String status;

    // Image URLs
    private String image1; // Mandatory
    private String image2;
    private String image3;
    private String image4;
    private String image5;

    

    @Column(name = "main_photo") // <-- Moved here
    private String mainPhoto;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Getters & Setters
    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImage1() { return image1; }
    public void setImage1(String image1) { this.image1 = image1; }
    public String getImage2() { return image2; }
    public void setImage2(String image2) { this.image2 = image2; }
    public String getImage3() { return image3; }
    public void setImage3(String image3) { this.image3 = image3; }
    public String getImage4() { return image4; }
    public void setImage4(String image4) { this.image4 = image4; }
    public String getImage5() { return image5; }
    public void setImage5(String image5) { this.image5 = image5; }

    
    public String getMainPhoto() { return mainPhoto; }
    public void setMainPhoto(String mainPhoto) { this.mainPhoto = mainPhoto; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
