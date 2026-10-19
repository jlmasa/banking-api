package com.jlmasa.banking.web;

import com.jlmasa.banking.domain.Account;
import com.jlmasa.banking.domain.LedgerEntry;
import com.jlmasa.banking.domain.Transfer;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class Dtos {

    private Dtos(){}

    public record CreateAccountRequest(@NotBlank @Size(max =60) String owner, @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 2)
                                       BigDecimal openingBalance){}

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount){}

    public record AccountResponse(Long id, String owner, BigDecimal balance){
        public static AccountResponse from(Account a){
            return new AccountResponse(a.getId(), a.getOwner(),a.getBalance());
        }
    }

    public record TransferResponse(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount, String status, Instant createdAt){
        public static TransferResponse from(Transfer t){
            return new TransferResponse(t.getId(), t.getFromAccount(), t.getToAccount(), t.getAmount(), t.getStatus(), t.getCreatedAt());
        }
    }

    public record LedgerEntryResponse(Long id, Long transferId, BigDecimal amount, String reason, Instant createAt){
        public static LedgerEntryResponse from (LedgerEntry e){
            return new LedgerEntryResponse(e.getId(), e.getTransferId(), e.getAmount(), e.getReason(), e.getCreatedAt());
        }
    }

}
