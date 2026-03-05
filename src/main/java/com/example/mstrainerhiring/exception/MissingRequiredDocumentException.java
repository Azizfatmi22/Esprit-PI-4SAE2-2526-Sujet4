package com.example.mstrainerhiring.exception;

public class MissingRequiredDocumentException extends RuntimeException {

    public MissingRequiredDocumentException(String message) {
        super(message);
    }
}
