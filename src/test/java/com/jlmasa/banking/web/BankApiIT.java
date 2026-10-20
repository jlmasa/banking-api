package com.jlmasa.banking.web;

import com.jlmasa.banking.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class BankApiIT extends IntegrationTestBase {

    private static final String PASSWORD ="dev-only-password";
    //the default app.demo.password

    @Autowired MockMvc mvc;


    private long open(String owner, String balance) throws Exception {
        String body = mvc.perform(post("/api/v1/accounts")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"owner\":\"" + owner + "\",\"openingBalance\":" + balance +"}"))
            .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions transfer(String user, String key, long from, long
            to, String amount) throws Exception {
        return mvc.perform(post("/api/v1/transfers")
                .with(httpBasic(user, PASSWORD))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":%s}".formatted(from,to,amount)));
    }

    private static String key() {return UUID.randomUUID().toString();}

    @Test
    void anonymousCallsGet401() throws Exception {
        mvc.perform(get("/api/v1/accounts/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/transfers")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminsCanOpenAccounts() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                .with(httpBasic("alice", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(" {\"owner\":\"alice\",\"openingBalance\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferThenRetryMovesMoneyOnce() throws Exception {
        long a = open("alice", "500.00");
        long b = open("bob", "100.00");
        String key = key();
        transfer("alice", key, a, b,
                "125.50").andExpect(status().isCreated());
        transfer("alice", key, a, b, "125.50")
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"));

        mvc.perform(get("/api/v1/accounts/" + a).with(httpBasic("alice", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(374.5));
    }

    @Test
    void sameKeyWithDifferentAmountIs409() throws Exception {
        long a = open("alice", "500.00");
        long b = open("bob", "100.00");
        String key = key();

        transfer("alice",key,a,b,"10.00").andExpect(status().isCreated());
        transfer("alice",key,a,b,"20.00").andExpect(status().isConflict());
    }

    @Test
    void strangersGet404NotAHintThatTheAccountExists() throws Exception {
        long a = open("alice", "500.00");
        mvc.perform(get("/api/v1/accounts/" + a). with(httpBasic("bob", PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/"+ a +"/ledger" ).with(httpBasic("bob", PASSWORD)))
                .andExpect(status().isNotFound());
    }
    @Test
    void spendingFromSomeoneElsesAccountIs403() throws Exception {
        long a = open("alice", "500.00");
        long b = open("bob", "100.00");
        transfer("bob", key(), a, b,
                "1.00").andExpect(status().isForbidden());
    }

    @Test
    void insufficientFundsIs422() throws Exception {
        long a = open("alice", "10.00");
        long b = open("bob", "0");
        transfer("alice", key(), a, b,"10.01").andExpect(status().isUnprocessableContent());

    }

    @Test
    void badAmountsAreRejectedAtTheEdge() throws Exception {
        long a = open("alice", "100.00");
        long b = open("bob", "0");

        for (String bad : new String[] {"10.001", "0", "-5"}){
            transfer("alice", key(), a,b,bad).andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingIdempotencyKeyIs400() throws Exception {
        long a = open("alice", "100.00");
        long b = open("bob", "0");

        mvc.perform(post("/api/v1/transfers")
                .with(httpBasic("alice", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(" {\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1.00}".formatted(a,b)))
                .andExpect(status().isBadRequest());
    }
}
