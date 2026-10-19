package com.jlmasa.banking.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity @Table(name = "audit_log")
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private String actor;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String detail;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected AuditLog(){}

    public AuditLog(String actor, String action, String detail){
        this.actor = actor;
        this.action = action;
        this.detail = detail;
    }
}
