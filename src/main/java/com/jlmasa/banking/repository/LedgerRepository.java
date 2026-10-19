package com.jlmasa.banking.repository;

import com.jlmasa.banking.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    Page<LedgerEntry> findByAccountId(Long accountId, Pageable pageable);

    @Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.accountId = :accountId")
    BigDecimal sumForAccount(@Param("accountId") Long accountId);

}
