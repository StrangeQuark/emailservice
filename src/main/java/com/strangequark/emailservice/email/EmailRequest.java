package com.strangequark.emailservice.email;

public class EmailRequest {
    private String recipient;
    private String sender;
    private String body;
    private String subject;
    private boolean includeToken;
    private String emailTemplateName;

    public EmailRequest() {

    }

    public EmailRequest(String recipient, String sender, String body, String subject, boolean includeToken) {
        this.recipient = recipient;
        this.sender = sender;
        this.body = body;
        this.subject = subject;
        this.includeToken = includeToken;
    }

    public EmailRequest(String recipient, String sender, boolean includeToken, String emailTemplateName) {
        this.recipient = recipient;
        this.sender = sender;
        this.includeToken = includeToken;
        this.emailTemplateName = emailTemplateName;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean getIncludeToken() {
        return includeToken;
    }

    public void setIncludeToken(boolean includeToken) {
        this.includeToken = includeToken;
    }

    public String getEmailTemplateName() {
        return emailTemplateName;
    }

    public void setEmailTemplateName(String emailTemplateName) {
        this.emailTemplateName = emailTemplateName;
    }
}
