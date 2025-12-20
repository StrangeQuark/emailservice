package com.strangequark.emailservice.template;

import java.util.Map;

public class EmailTemplateRequest {
    private String recipient;
    private String sender;
    private boolean includeToken;
    private String templateName;
    private Map<String, String> templateVariables;

    public EmailTemplateRequest() {

    }
    public EmailTemplateRequest(String recipient, String sender, boolean includeToken, String templateName, Map<String, String> templateVariables) {
        this.recipient = recipient;
        this.sender = sender;
        this.includeToken = includeToken;
        this.templateName = templateName;
        this.templateVariables = templateVariables;
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

    public boolean getIncludeToken() {
        return includeToken;
    }

    public void setIncludeToken(boolean includeToken) {
        this.includeToken = includeToken;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
    }
}
