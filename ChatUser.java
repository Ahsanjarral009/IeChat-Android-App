package com.example.iechat;

public class ChatUser {
    private String username;
    private String lastMessage;
    private int profileResId;

    public ChatUser(String username, String lastMessage, int profileResId) {
        this.username = username;
        this.lastMessage = lastMessage;
        this.profileResId = profileResId;
    }

    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public int getProfileResId() { return profileResId; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
}
