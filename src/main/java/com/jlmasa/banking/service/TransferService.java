package com.jlmasa.banking.service;

import com.jlmasa.banking.domain.*;
import com.jlmasa.banking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerRepository ledger;
    private final AuditLogRepository audit;

    public TransferService(AccountRepository accounts, TransferRepository transfers, LedgerRepository ledger, AuditLogRepository audit){
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
        this.audit = audit;
    }

    @Transactional
    public TransferResult execute(String caller, String key, Long fromId, Long toId, BigDecimal rawAmount){
        if(fromId.equals(toId)){
            throw new IllegalArgumentException("Cannot Transfer to the same account");
        }
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.UNNECESSARY);
        //10 -> 10.00; 10.001 is rejected
        String hash = fingerprint(fromId, toId, amount);
        //1. Has this user already send this key? replay the stored result, or reject a changed request.
        var existing = transfers.findByRequestedByAndIdempotencyKey(caller, key);
        if(existing.isPresent()){
            return replay(existing.get(), hash);
        }
        //2. Lock Both Accounts, always the lower id first, so opposite transfers cannot deadlock
        Account first = lock(Math.min(fromId, toId));
        Account second = lock(Math.max(fromId, toId));
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = from == first ? second : first;

        //2b Re-check the key now that we hold the locks. A parallel request with the same key may have
        //committed while we waited; replaying it here is clearer then failing on the unique constraint
        var raced = transfers.findByRequestedByAndIdempotencyKey(caller, key);
        if (raced.isPresent()){
            return replay(raced.get(), hash);
        }

        //3. Authorization: only the owner can spend from the source account.
        if(!from.getOwner().equals(caller)){
            throw new NotAccountOwnerException();
        }
        //4. Move the money. the entity enforces positive amounts and non-negative balances.
        from.debit(amount);
        to.credit(amount);
        //5. insert the transfer. If a parallel request with the same key commited first.
        // the unique constraints fails here and the whole transaction rolls back;
        Transfer transfer = transfers.saveAndFlush(new Transfer(caller, key, hash,fromId,toId,amount));

        //6. Double-entry style history, plus an audit trail
        ledger.save(new LedgerEntry(transfer.getId(), fromId, amount.negate(), "TRANSFER OUT"));
        ledger.save(new LedgerEntry(transfer.getId(), toId, amount,"TRANSFER_IN"));
        audit.save(new AuditLog(caller, "TRANSFER", "transfer="+ transfer.getId() + " from=" + fromId + " to="+toId+ " amount=" + amount));
        return new TransferResult(transfer, false);

        /** Used by the facade after losing a race on the idempotency key. */


    }

    @Transactional(readOnly = true)
    public TransferResult replayExisting(String caller, String key, Long fromId, Long toId, BigDecimal rawAmount){
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.UNNECESSARY);
        return transfers.findByRequestedByAndIdempotencyKey(caller,key)
                .map(t -> replay(t, fingerprint(fromId, toId,amount)))
                .orElseThrow(() -> new IllegalStateException("Transfer vanished after key conflict"));
    }
    private TransferResult replay(Transfer stored, String hash){
        if (!stored.getRequestHash().equals(hash)){
            throw new IdempotencyConflictException();
        }
        return new TransferResult(stored, true);
    }
    private Account lock(Long id){
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
    }
    private static String fingerprint(Long from, Long to, BigDecimal amount){
        try{
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((from + "|" + to +"|" + amount.toPlainString()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }catch(NoSuchAlgorithmException e){
            throw new IllegalStateException(e);
        }
    }
}
