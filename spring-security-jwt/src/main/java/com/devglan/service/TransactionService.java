package com.devglan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devglan.model.Transaction;
import com.devglan.model.User;
import com.devglan.repository.TransactionRepository;

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
    
    public void createTransaction(User user, String transactionType, BigDecimal amount, String remark, String fromTo) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        LocalDateTime now = LocalDateTime.now();
        Date currentDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        transaction.setTransactionDate(currentDate);
        transaction.setStatus("done");
        transaction.setRemark(remark);
        transaction.setFromTo(fromTo);
        transaction.setBalanceAfterTransaction(user.getBalance());
        transactionRepository.save(transaction);
    }

}
