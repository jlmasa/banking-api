package com.jlmasa.banking.web;

import com.jlmasa.banking.domain.Account;
import com.jlmasa.banking.service.AccountService;
import com.jlmasa.banking.service.TransferFacade;
import com.jlmasa.banking.service.TransferResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.jlmasa.banking.web.Dtos.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Validated
public class BankController {

    private final AccountService accounts;
    private final TransferFacade transfers;

    public BankController(AccountService accounts, TransferFacade transfers){
        this.accounts = accounts;
        this.transfers = transfers;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody CreateAccountRequest req, Authentication auth){
        Account created = accounts.open(auth.getName(), req.owner(), req.openingBalance());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + created.getId())).body(AccountResponse.from(created));
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable Long id, Authentication auth){
        return AccountResponse.from(accounts.getOwned(id, auth.getName()));
    }

    @GetMapping("/accounts/{id}/ledger")
    public Page<LedgerEntryResponse> ledger(@PathVariable Long id, Authentication auth, @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)Pageable pageable){
        return accounts.ledger(id, auth.getName(), pageable).map(LedgerEntryResponse::from);
    }
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(Authentication auth, @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 80) String key,
    @Valid @RequestBody TransferRequest req){
        TransferResult result = transfers.transfer(
                auth.getName(), key, req.fromAccountId(), req.toAccountId(), req.amount()
        );
        TransferResponse body = TransferResponse.from(result.transfer());
        if(result.replayed()){
            return ResponseEntity.ok().header("Idempotent-Replay", "true").body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
        //201: money moved.
    }
}
