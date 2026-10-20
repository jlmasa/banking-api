package com.jlmasa.banking.web;

import com.jlmasa.banking.domain.Account;
import com.jlmasa.banking.service.AccountService;
import com.jlmasa.banking.service.TransferFacade;
import com.jlmasa.banking.service.TransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Move money between two accounts", description = "Safe to retry : repeating request with the same Idempotency-key returns the original Results.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Money moved"), @ApiResponse(responseCode = "200", description = "Repeat of an earlier request; nothing moved again"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, ids or missing key"),@ApiResponse(responseCode = "403", description = "Source account belongs to someone else"),
            @ApiResponse(responseCode = "409", description = "Key already used with a different request"), @ApiResponse(responseCode = "422", description = "insufficient funds")
    })
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(Authentication auth, @Parameter(description = "Unique per logical request, for example a UUID")
                                                     @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 80) String key,
                                                     @Valid @RequestBody TransferRequest req){
        TransferResult result = transfers.transfer(auth.getName(), key, req.fromAccountId(), req.toAccountId(), req.amount());
        TransferResponse body = TransferResponse.from(result.transfer());
        if(result.replayed()){
            return ResponseEntity.ok().header("Idempotent-Replay", "true").body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
