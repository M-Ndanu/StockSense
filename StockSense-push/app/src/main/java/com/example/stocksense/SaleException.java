package com.example.stocksense;

/**
 * Thrown when a sale cannot be recorded (empty sale, not enough stock, unknown product).
 * The message is written for the user, so screens can show it directly in a Toast.
 */
public class SaleException extends Exception {
    private static final long serialVersionUID = 1L;

    // Builds the exception with a user-friendly message.
    public SaleException(String message) {
        super(message);
    }
}
