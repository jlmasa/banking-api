package com.jlmasa.banking.domain;

public class AccountNotFoundException extends RuntimeException{
    public AccountNotFoundException(Long id) {
        super("Account "+ id + "not found");
    }
}
