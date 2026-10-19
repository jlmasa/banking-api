package com.jlmasa.banking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String owner;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal balance;
    @Version private long version;

    protected Account() {}

    public Account(String owner, BigDecimal openingBalance){
        if (openingBalance == null || openingBalance.signum() <0 ){
            throw new IllegalArgumentException("Opening balance must not be negative");
        }
        this.owner = owner;
        this.balance = openingBalance;
    }
    public void debit(BigDecimal amount){
        requirePositive(amount);
        if(balance.compareTo(amount) <0) throw new InsufficientFundsException();
        balance = balance.subtract(amount);
    }
    public void credit(BigDecimal amount){
        requirePositive(amount);
        balance = balance.add(amount);
    }
    private static void requirePositive(BigDecimal amount){
        if (amount == null || amount.signum() <=0 ){
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    public Long getId() {return id;}
    public String getOwner(){return owner;}
    public BigDecimal getBalance() { return balance;}
}
