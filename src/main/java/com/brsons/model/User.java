package com.brsons.model;



import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    @NotBlank(message = "Phone number is required")
    @Column(unique = true)
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    private String status = "ACTIVE";
    
    @Column(name = "type")
    private String type = "Retail"; // Example values: "Admin", "Customer"
    private String role = "USER";
    
    // Address Information for Tax Type Determination
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "zip_code")
    private String zipCode;
    
    @Column(name = "gstin")
    private String gstin;

    // Getter & Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatus() { return status; }
    public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public void setStatus(String status) { this.status = status; }
	
	// Address Getters & Setters
	public String getAddressLine1() { return addressLine1; }
	public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
	
	public String getAddressLine2() { return addressLine2; }
	public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
	
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }
	
	public String getState() { return state; }
	public void setState(String state) { this.state = state; }
	
	public String getZipCode() { return zipCode; }
	public void setZipCode(String zipCode) { this.zipCode = zipCode; }
	
	public String getGstin() { return gstin; }
	public void setGstin(String gstin) { this.gstin = gstin; }
	
	// Helper method to get full address
	public String getFullAddress() {
		StringBuilder address = new StringBuilder();
		if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
			address.append(addressLine1);
		}
		if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
			if (address.length() > 0) address.append(", ");
			address.append(addressLine2);
		}
		if (city != null && !city.trim().isEmpty()) {
			if (address.length() > 0) address.append(", ");
			address.append(city);
		}
		if (state != null && !state.trim().isEmpty()) {
			if (address.length() > 0) address.append(", ");
			address.append(state);
		}
		if (zipCode != null && !zipCode.trim().isEmpty()) {
			if (address.length() > 0) address.append(" - ");
			address.append(zipCode);
		}
		return address.toString();
	}
}
