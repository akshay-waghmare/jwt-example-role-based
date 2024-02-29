package com.devglan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devglan.dao.TransactionRepository;
import com.devglan.model.Transaction;

import javassist.NotFoundException;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long id) throws NotFoundException {
        return transactionRepository.findById(id)
                                    .orElseThrow(() -> new NotFoundException("Transaction not found with id: " + id));
    }

    // Add other service methods as needed

}
