package com.strangequark.emailservice.email;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
