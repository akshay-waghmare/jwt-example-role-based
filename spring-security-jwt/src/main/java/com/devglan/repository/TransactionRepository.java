package com.devglan.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devglan.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate AND t.user.id = :userId")
    List<Transaction> findTransactionsBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("userId") Long userId);

    List<Transaction> findByRemarkContainingAndFromToAndStatus(String normalizedMatchUrl, String fromTo, String status);


}
