package com.langfree.model;

import java.util.Date;

public class Message {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ANA = 1;
    
    private long id;
    private int type; // TYPE_USER ou TYPE_ANA
    private String text;
    private Correction correction;
    private long timestamp;
    
    public Message() {
        this.timestamp = new Date().getTime();
    }
    
    public Message(int type, String text) {
        this.type = type;
        this.text = text;
        this.timestamp = new Date().getTime();
    }
    
    public Message(int type, String text, Correction correction) {
        this.type = type;
        this.text = text;
        this.correction = correction;
        this.timestamp = new Date().getTime();
    }
    
    // Getters
    public long getId() { return id; }
    public int getType() { return type; }
    public String getText() { return text; }
    public Correction getCorrection() { return correction; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setId(long id) { this.id = id; }
    public void setType(int type) { this.type = type; }
    public void setText(String text) { this.text = text; }
    public void setCorrection(Correction correction) { this.correction = correction; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isUserMessage() { return type == TYPE_USER; }
    public boolean isAnaMessage() { return type == TYPE_ANA; }
}
