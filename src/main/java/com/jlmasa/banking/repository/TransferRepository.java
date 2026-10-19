package com.jlmasa.banking.repository;

import com.jlmasa.banking.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByRequestedByAndIdempotencyKey(String requestedBy, String idempotencyKey);
}
