package com.example.iechat;

import java.util.Objects;

public class ChatMessage {
    private String message;
    private boolean isSent;
    private String timestamp;

    public ChatMessage(String message, boolean isSent, String timestamp) {
        this.message = message;
        this.isSent = isSent;
        this.timestamp = timestamp;
    }

    public String getMessage() { return message; }
    public boolean isSent() { return isSent; }
    public String getTimestamp() { return timestamp; }
    public String getTime() { return timestamp; }

    // Add this setter method to update the timestamp
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        ChatMessage msg = (ChatMessage) obj;
        return isSent == msg.isSent &&
                timestamp.equals(msg.timestamp) &&
                message.equals(msg.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, isSent, timestamp);
    }
}