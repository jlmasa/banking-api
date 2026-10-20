package com.jlmasa.banking.service;

import com.jlmasa.banking.IntegrationTestBase;
import com.jlmasa.banking.domain.Account;
import com.jlmasa.banking.domain.IdempotencyConflictException;
import com.jlmasa.banking.domain.InsufficientFundsException;
import com.jlmasa.banking.domain.NotAccountOwnerException;
import com.jlmasa.banking.repository.AccountRepository;
import com.jlmasa.banking.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class TransferServiceIT extends IntegrationTestBase {

    @Autowired AccountService accounts;
    @Autowired TransferFacade transfers;
    @Autowired
    AccountRepository accountRepo;
    @Autowired
    LedgerRepository ledger;

    private Account alice;
    private Account bob;

    @BeforeEach
    void openAccounts(){
        alice = accounts.open("admin", "alice", new BigDecimal("1000.00"));
        bob  = accounts.open("admin", "bob", new BigDecimal("1000.00"));
    }
    private static String key() {return UUID.randomUUID().toString();}

    private BigDecimal balanceOf(Account a){
        return accountRepo.findById(a.getId()).orElseThrow().getBalance();
    }
    private void assertLedgerMatchesBalance(Account a){
        assertThat(ledger.sumForAccount(a.getId())).isEqualByComparingTo(balanceOf(a));
    }
    @Test
    void transferMovesMoneyAndKeepLedgerInSync(){
        TransferResult result = transfers.transfer("alice", key(), alice.getId(), bob.getId(), new BigDecimal("125.50"));

        assertThat(result.replayed()).isFalse();
        assertThat(balanceOf(alice)).isEqualByComparingTo("874.50");
        assertThat(balanceOf(bob)).isEqualByComparingTo("1125.50");
        assertLedgerMatchesBalance(alice);
        assertLedgerMatchesBalance(bob);
    }

    @Test
    void sameKeyTwiceMovesMoneyOnlyOnce(){
        String key = key();
        TransferResult first = transfers.transfer("alice", key, alice.getId(), bob.getId(), new BigDecimal("100.00"));
        TransferResult second  = transfers.transfer("alice", key, alice.getId(), bob.getId(), new BigDecimal(100.00));

        assertThat(second.replayed()).isTrue();

        assertThat(second.transfer().getId()).isEqualTo(first.transfer().getId());
        assertThat(balanceOf(alice)).isEqualByComparingTo("900.00");
    }

    @Test
    void sameKeyWithDifferentAmountIsRejected(){
        String key = key();
        transfers.transfer("alice", key, alice.getId(), bob.getId(), new BigDecimal("100.00"));

        assertThatThrownBy(() -> transfers.transfer("alice", key, alice.getId(), bob.getId(), new BigDecimal("999.00"))).isInstanceOf(IdempotencyConflictException.class);
        assertThat(balanceOf(alice)).isEqualByComparingTo("900.00");
    }
    @Test
    void insufficientFundsLeavesBothBalancesUntouched() {
        assertThatThrownBy(() -> transfers.transfer("alice", key(), alice.getId(), bob.getId(),new BigDecimal("1000.01"))).isInstanceOf(
                InsufficientFundsException.class
        );
        assertThat(balanceOf(alice)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bob)).isEqualByComparingTo("1000.00");
    }

    @Test
    void cannotSpendFromSomeoneElsesAccount() {
        assertThatThrownBy(() -> transfers.transfer("bob", key(), alice.getId(), bob.getId(), BigDecimal.TEN)).isInstanceOf(
                NotAccountOwnerException.class
        );
        assertThat(balanceOf(alice)).isEqualByComparingTo("1000.00");
    }

    @Test
    void eightParallelRequestsWithOneKeyMoveMoneyOnce() throws Exception {
        String key = key();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TransferResult>> futures = new ArrayList<>();
        for(int i = 0; i< 8; i++){
            futures.add(pool.submit(() -> {
                start.await();
                //release all threads at the same instant
                return transfers.transfer("alice", key, alice.getId(), bob.getId(), new BigDecimal("100.00"));
            }));
        }
        start.countDown();

        Set<Long> transferIds = new HashSet<>();
        int replays = 0;

        for (Future<TransferResult> f : futures){
            TransferResult r = f.get(30, TimeUnit.SECONDS);
            transferIds.add(r.transfer().getId());
            if(r.replayed()) replays++;
        }
        pool.shutdown();
        assertThat(transferIds).hasSize(1);
        //everyone got the same transfer back
        assertThat(replays).isEqualTo(7);
        //one winner seven replays
        assertThat(balanceOf(alice)).isEqualByComparingTo("900.00");
        assertThat(balanceOf(bob)).isEqualByComparingTo("1100.00");
    }
    @Test
    void oppositeTransfersNeverDeadlockAndMoneyIsConserved() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<TransferResult>> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++){
            jobs.add(() -> transfers.transfer("alice", key(), alice.getId(), bob.getId(), BigDecimal.ONE));
            jobs.add(() -> transfers.transfer("bob", key(), bob.getId(), alice.getId(), BigDecimal.ONE));
        }
        List<Future<TransferResult>> results = pool.invokeAll(jobs, 60, TimeUnit.SECONDS);
        pool.shutdown();
        for(Future<TransferResult> f: results){
            f.get(); //rethrows a failure; a job cutoff by the timeout (deadlock) throws cancellationException
        }
        assertThat(balanceOf(alice)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bob)).isEqualByComparingTo("1000.00");
        assertLedgerMatchesBalance(alice);
        assertLedgerMatchesBalance(bob);
    }
}
