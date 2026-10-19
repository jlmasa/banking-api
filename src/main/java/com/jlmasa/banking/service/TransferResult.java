package com.jlmasa.banking.service;

import com.jlmasa.banking.domain.Transfer;

public record TransferResult(Transfer transfer, boolean replayed) {
}
