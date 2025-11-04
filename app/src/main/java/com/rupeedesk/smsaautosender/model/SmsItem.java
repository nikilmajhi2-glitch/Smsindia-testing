package com.rupeedesk.smsaautosender.model;

import java.util.Date;

public class SmsItem {
    private String id;
    private String recipient;
    private String message;
    private long scheduledTime;
    private boolean sent;
    private String sentBy;
    private Date addedAt;
    private Date sentAt;
    private String userId;  // added userId field

    public SmsItem() {}

    public SmsItem(String id, String recipient, String message, String userId) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.sent = false;
        this.scheduledTime = System.currentTimeMillis();
        this.userId = userId;
    }

    // Getters and setters...
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public String getSentBy() { return sentBy; }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }

    public Date getAddedAt() { return addedAt; }
    public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
}