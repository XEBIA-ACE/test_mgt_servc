package com.example.usermanagement.exception;

public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException() {
        super("Invalid username/email or password");
    }
}
