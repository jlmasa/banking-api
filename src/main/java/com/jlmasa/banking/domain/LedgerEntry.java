package com.jlmasa.banking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "transfer_id") private Long transferId;
    @Column(name = "account_id", nullable = false ) private Long accountId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String reason;
    @Column(name="created_at", nullable = false) private Instant createdAt = Instant.now();

    protected LedgerEntry(){}

    public LedgerEntry(Long transferId, Long accountId, BigDecimal amount, String reason){
        this.transferId = transferId;
        this.accountId = accountId;
        this.amount = amount;
        this.reason = reason;
    }

    public Long getId(){return id;}
    public Long getTransferId() {return transferId;}
    public Long getAccountId() {return accountId;}
    public BigDecimal getAmount() {return amount;}
    public String getReason() {return reason;}
    public Instant getCreatedAt() {return createdAt;}

}
