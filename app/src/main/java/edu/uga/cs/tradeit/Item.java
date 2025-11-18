package edu.uga.cs.tradeit;

public class Item {
    private String key; // Firebase generates unique keys of String type
    private String sellerId; // foreign key ID string of seller user
    private String name;
    private String categoryId;
    private long creationTime; // do not use DateTime or JSON objects with realtime databases
    private double price;
    private String buyerId;
    private String status; // "available" or "pending" or "sold"

    public Item() {
        this.key = null;
        this.sellerId = null;
        this.name = null;
        this.categoryId = null;
        this.creationTime = 0;
        this.price = 0;
        this.buyerId = null;
        this.status = null;
    }

    public Item(String sellerId, String name, String categoryId, long creationTime, double price, String status) {
        this.key = null;
        this.sellerId = sellerId;
        this.name = name;
        this.categoryId = categoryId;
        this.creationTime = creationTime;
        this.price = price;
        this.buyerId = null;
        this.status = status;
    }

    public String getKey() {return key;}
    public void setKey(String key) {this.key = key;}
    public String getSellerId() {return sellerId;}
    public void setSellerId(String sellerId) {this.sellerId = sellerId;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getCategory() {return categoryId;}
    public void setCategory(String categoryId) {this.categoryId = categoryId;}
    public double getCreationTime() {return creationTime;}
    public void setCreationTime(long creationTime) {this.creationTime = creationTime;}
    public double getPrice() {return price;}
    public void setPrice(double price) {this.price = price;}
    public String getBuyerId() {return buyerId;}
    public void setBuyerId(String buyerId) {this.buyerId = buyerId;}
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
}
