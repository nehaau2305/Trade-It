package edu.uga.cs.tradeit;

public class Item {

    private String key;
    private String sellerId;
    private String name;
    private String categoryId;
    private long creationTime;
    private double price;
    private String buyerId;
    private String status;
    private String description;
    private boolean buyerConfirmed;
    private boolean sellerConfirmed;

    // NEW: snapshot of the category title (for history / completed transactions)
    private String categoryTitle;

    public Item() {
        this.key = null;
        this.sellerId = null;
        this.name = null;
        this.categoryId = null;
        this.creationTime = 0L;
        this.price = 0.0;
        this.buyerId = null;
        this.status = null;
        this.description = null;
        this.buyerConfirmed = false;
        this.sellerConfirmed = false;
        this.categoryTitle = null;
    }

    public Item(String sellerId,
                String name,
                String categoryId,
                long creationTime,
                double price,
                String status,
                String description) {

        this.key = null;
        this.sellerId = sellerId;
        this.name = name;
        this.categoryId = categoryId;
        this.creationTime = creationTime;
        this.price = price;
        this.buyerId = null;
        this.status = status;
        this.description = description;
        this.buyerConfirmed = false;
        this.sellerConfirmed = false;
        this.categoryTitle = null; // set separately
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    // you still have these helpers
    public String getCategory() { return categoryId; }
    public void setCategory(String categoryId) { this.categoryId = categoryId; }

    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isBuyerConfirmed() { return buyerConfirmed; }
    public void setBuyerConfirmed(boolean buyerConfirmed) { this.buyerConfirmed = buyerConfirmed; }

    public boolean isSellerConfirmed() { return sellerConfirmed; }
    public void setSellerConfirmed(boolean sellerConfirmed) { this.sellerConfirmed = sellerConfirmed; }

    // NEW: categoryTitle snapshot
    public String getCategoryTitle() { return categoryTitle; }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }
}
