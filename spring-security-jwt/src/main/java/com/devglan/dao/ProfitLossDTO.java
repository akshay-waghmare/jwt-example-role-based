package com.devglan.dao;

import java.math.BigDecimal;
import java.util.Date;

public class ProfitLossDTO {
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private String remark;
    private String status;
    private Date transactionDate;
    private String transactionType;

    public ProfitLossDTO(BigDecimal amount, BigDecimal balanceAfterTransaction, String remark, String status, Date transactionDate, String transactionType) {
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.remark = remark;
        this.status = status;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(BigDecimal balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}
