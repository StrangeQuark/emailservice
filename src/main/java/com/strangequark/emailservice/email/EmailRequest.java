package com.strangequark.emailservice.email;
public class EmailRequest {
    private String recipient;
    private String sender;
    private String email;
    private String subject;

    public EmailRequest(String recipient, String sender, String email, String subject) {
        this.recipient = recipient;
        this.sender = sender;
        this.email = email;
        this.subject = subject;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
