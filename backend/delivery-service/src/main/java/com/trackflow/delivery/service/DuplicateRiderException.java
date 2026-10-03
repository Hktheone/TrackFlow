package com.trackflow.delivery.service;

public class DuplicateRiderException extends RuntimeException {

    public DuplicateRiderException(String username) {
        super("A rider profile for '" + username + "' already exists");
    }
}
