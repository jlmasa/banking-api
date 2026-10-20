package com.jlmasa.banking.service;

import com.jlmasa.banking.domain.Account;
import com.jlmasa.banking.domain.AccountNotFoundException;
import com.jlmasa.banking.domain.AuditLog;
import com.jlmasa.banking.domain.LedgerEntry;
import com.jlmasa.banking.repository.AccountRepository;
import com.jlmasa.banking.repository.AuditLogRepository;
import com.jlmasa.banking.repository.LedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final LedgerRepository ledger;
    private final AuditLogRepository audit;

    public AccountService(AccountRepository accounts,LedgerRepository ledger, AuditLogRepository audit){
    this.accounts = accounts;
    this.ledger = ledger;
    this.audit = audit;
    }

    @Transactional
    public Account open(String actor, String owner, BigDecimal
            openingBalance) {
        Account account = accounts.save(new Account(owner, openingBalance));
        if (openingBalance.signum() > 0) {
// the opening deposit is a ledger entry too, so balance ==sum(ledger) always holds
            ledger.save(new LedgerEntry(null, account.getId(),
                    openingBalance, "OPENING_DEPOSIT"));
        }
        audit.save(new AuditLog(actor, "ACCOUNT_OPENED", "account=" +
                account.getId() + " owner=" + owner));
        return account;
    }

    /** Returns the account only if the caller owns it. Strangers get "not found", not "forbidden". */

    @Transactional(readOnly = true)
    public Account getOwned(Long id, String caller){
        return accounts.findById(id)
                .filter(a -> a.getOwner().equals(caller))
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> ledger(Long id, String caller, Pageable
            pageable) {
        getOwned(id, caller);
        return ledger.findByAccountId(id, pageable);
    }

}
