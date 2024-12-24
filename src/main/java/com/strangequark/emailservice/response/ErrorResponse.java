package com.strangequark.emailservice.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response object for errors
 */
public class ErrorResponse {
    /**
     * Timestamp of when the response occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private final LocalDateTime timestamp;

    /**
     * Message included in the response
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String message;

    /**
     * Code for the response
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private int errorCode;

    /**
     * Default constructor, set the timestamp
     */
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor if only message is passed
     */
    public ErrorResponse(String message) {
        this();
        this.message = message;
    }

    /**
     * Constructor if only errorCode is passed
     */
    public ErrorResponse(int errorCode) {
        this();
        this.errorCode = errorCode;
    }

    /**
     * Constructor if both message and errorCode are passed
     */
    public ErrorResponse(String message, int errorCode) {
        this(message);
        this.errorCode = errorCode;
    }
}
