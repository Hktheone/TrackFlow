package com.trackflow.gateway.auth.service;

/** Failures the auth API turns into specific HTTP statuses. */
public final class AuthExceptions {

    private AuthExceptions() {
    }

    /** Wrong username, wrong password and disabled account all look the same to the caller. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid username or password");
        }
    }

    public static class UsernameTakenException extends RuntimeException {
        public UsernameTakenException(String username) {
            super("Username '" + username + "' is already taken");
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(Object id) {
            super("User " + id + " not found");
        }
    }

    public static class InvalidUserChangeException extends RuntimeException {
        public InvalidUserChangeException(String message) {
            super(message);
        }
    }
}
