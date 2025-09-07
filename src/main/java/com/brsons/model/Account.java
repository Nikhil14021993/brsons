package com.brsons.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;   // e.g., "1001"
    private String name;   // e.g., "Cash"
    private String type;   // ASSET, LIABILITY, INCOME, EXPENSE, EQUITY
    private String description;
    private boolean isActive = true;
    
    // Parent-Child relationship for sub-accounts
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"subAccounts", "parent"})
    private Account parent;
    
    @OneToMany(mappedBy = "parent")
    @JsonIgnoreProperties({"subAccounts", "parent"})
    private List<Account> subAccounts;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Account getParent() {
        return parent;
    }
    
    public void setParent(Account parent) {
        this.parent = parent;
    }
    
    public List<Account> getSubAccounts() {
        return subAccounts;
    }
    
    public void setSubAccounts(List<Account> subAccounts) {
        this.subAccounts = subAccounts;
    }
    
    // Helper methods
    public boolean isParentAccount() {
        return parent == null;
    }
    
    public boolean isSubAccount() {
        return parent != null;
    }
    
    public String getFullName() {
        if (parent != null) {
            return parent.getName() + " - " + name;
        }
        return name;
    }
    
    public String getFullCode() {
        if (parent != null) {
            return parent.getCode() + "." + code;
        }
        return code;
    }
}

