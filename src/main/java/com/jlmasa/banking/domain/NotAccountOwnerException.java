package com.jlmasa.banking.domain;

public class NotAccountOwnerException extends RuntimeException{
    public NotAccountOwnerException() {super("You do not own the source account");}
}
