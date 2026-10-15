package com.studysync.exception;

/** Thrown when a comma-separated profile field is invalid. The controller shows it next to that field. */
public class InvalidProfileInputException extends RuntimeException {
    private final String field;

    public InvalidProfileInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
