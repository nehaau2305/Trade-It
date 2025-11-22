package edu.uga.cs.tradeit;
public class Item {

    private String key;

    private String sellerId;

    private String name;

    private String categoryId;
    private String categoryTitle;
    private long creationTime;

    // price (0.0 means free)
    private double price;

    private String buyerId;

    // "available", "pending", or "sold"
    private String status;

    public Item() {
    }

    public Item(String sellerId,
                String name,
                String categoryId,
                String categoryTitle,
                long creationTime,
                double price,
                String status) {

        this.key = null;
        this.sellerId = sellerId;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryTitle = categoryTitle;
        this.creationTime = creationTime;
        this.price = price;
        this.buyerId = null;
        this.status = status;
    }

    // ----- getters and setters -----
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategory() { return categoryId; }
    public void setCategory(String categoryId) { this.categoryId = categoryId; }
    public String getCategoryTitle() { return categoryTitle; }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }

    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
