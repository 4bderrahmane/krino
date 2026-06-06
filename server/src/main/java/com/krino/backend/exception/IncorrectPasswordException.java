package com.krino.backend.exception;

public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("The password provided is incorrect.");
    }

    public IncorrectPasswordException(String message) {
        super(message);
    }
}
