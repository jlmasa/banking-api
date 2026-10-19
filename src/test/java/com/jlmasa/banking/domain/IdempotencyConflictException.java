package com.jlmasa.banking.domain;

public class IdempotencyConflictException extends RuntimeException{
    public IdempotencyConflictException() {
        super("Idempotency-key was already used with a different request");
    }
}
