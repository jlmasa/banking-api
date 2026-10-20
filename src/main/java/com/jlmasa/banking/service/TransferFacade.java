package com.jlmasa.banking.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Deliberately not @Transactional: it must run after the failed transaction has ended. */
@Service
public class TransferFacade {

    private final TransferService service;

    public TransferFacade(TransferService service){

        this.service = service;
    }
    public TransferResult transfer(String caller, String key, Long fromId, Long toId, BigDecimal amount){
        try{
            return service.execute(caller, key, fromId, toId, amount);
        }catch(DataIntegrityViolationException e){
            //A parallel request with the same key won, Return its stored Result.
            //If nothing is stored, the violation was something else (for Example a CHECK): rethrow it
            try {
                return service.replayExisting(caller, key,fromId,toId,amount);
            }catch (IllegalStateException notFound){
                throw e;
            }
        }
    }
}
