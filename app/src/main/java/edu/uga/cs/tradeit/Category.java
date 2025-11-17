package edu.uga.cs.tradeit;

public class Category {
    private String key;
    private String creatorId;
    private String title;
    private long dateTime;

    public Category() {
        this.key = null;
        this.creatorId = null;
        this.title = null;
        this.dateTime = 0;
    }

    public Category(String creatorId, String title, long dateTime) {
        this.key = null;
        this.creatorId = creatorId;
        this.title = title;
        this.dateTime = dateTime;
    }

    public String getKey() {return key;}
    public void setKey(String key) {this.key = key;}
    public String getCreatorId() {return creatorId;}
    public void setCreatorId(String creatorId) {this.creatorId = creatorId;}
    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}
    public long getDateTime() {return dateTime;}
    public void setDateTime(long dateTime) {this.dateTime = dateTime;}

}
