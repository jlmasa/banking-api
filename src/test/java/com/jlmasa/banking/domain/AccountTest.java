package com.jlmasa.banking.domain;


import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class AccountTest {

    private Account account(String balance){
        return new Account("alice", new BigDecimal(balance));
    }
    @Test
    void debitReducesBalance(){
        var a = account("100.00");
        a.debit(new BigDecimal("30.00"));
        assertThat(a.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void debitOfTheExactBalanceLeaveZero() {
        var a = account("10.00");
        a.debit(new BigDecimal(10.00));
        assertThat(a.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void debitBeyondBalanceIsRejectedAndBalanceIsUnchanged(){
        var a = account("10.00");
        assertThatThrownBy(()-> a.debit(new BigDecimal("10.01"))).isInstanceOf(InsufficientFundsException.class);
        assertThat(a.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test
    void creditIncreaseBalance(){
        var a = account("10.00");
        a.credit(new BigDecimal("0.10"));
        assertThat(a.getBalance()).isEqualByComparingTo("10.10");
    }
    @Test
    void zeroAndNegativeAmountsAreRejected(){
        var a = account("10.00");
        assertThatThrownBy(() -> a.debit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.credit(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void negativeOpeningBalanceIsRejected(){
        assertThatThrownBy(()-> account("-5.00")).isInstanceOf(IllegalArgumentException.class);
    }
}
