package com.strangequark.emailservice.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private final LocalDateTime timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String token;

    public Response() {
        this.timestamp = LocalDateTime.now();
    }

    public Response(String message) {
        this();
        this.message = message;
    }

    public Response(String message, String token) {
        this(message);
        this.token = token;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
