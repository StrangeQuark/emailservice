package com.strangequark.emailservice.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response object for errors
 */
public class SuccessResponse {
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
     * Default constructor, set the timestamp
     */
    public SuccessResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor if only errorMessage is passed
     */
    public SuccessResponse(String message) {
        this();
        this.message = message;
    }
}
