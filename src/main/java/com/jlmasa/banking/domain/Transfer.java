package com.jlmasa.banking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "transfers")
public class Transfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "requested_by", nullable = false) private String requestedBy;
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(name="request_hash", nullable = false) private String requestHash;
    @Column(name="from_account", nullable = false) private Long fromAccount;
    @Column(name="to_account", nullable = false) private Long toAccount;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String status = "COMPLETED";
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected Transfer(){}

    public Transfer(String requestedBy, String idempotencyKey, String requestHash, Long fromAccount, Long toAccount, BigDecimal amount){
        this.requestedBy = requestedBy;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }

    public Long getId() {return id;}
    public String getRequestHash() {return requestHash;}
    public Long getFromAccount() {return fromAccount;}
    public Long getToAccount() {return toAccount;}
    public BigDecimal getAmount() {return amount;}
    public String getStatus() {return status;}
    public Instant getCreatedAt() {return createdAt;}

}
