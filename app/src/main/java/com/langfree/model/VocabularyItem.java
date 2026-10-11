package com.langfree.model;

import java.util.Date;

public class VocabularyItem {
    private long id;
    private String english;
    private String french;
    private long timestamp;
    
    public VocabularyItem() {
        this.timestamp = new Date().getTime();
    }
    
    public VocabularyItem(String english, String french) {
        this.english = english;
        this.french = french;
        this.timestamp = new Date().getTime();
    }
    
    // Getters
    public long getId() { return id; }
    public String getEnglish() { return english; }
    public String getFrench() { return french; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setId(long id) { this.id = id; }
    public void setEnglish(String english) { this.english = english; }
    public void setFrench(String french) { this.french = french; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
