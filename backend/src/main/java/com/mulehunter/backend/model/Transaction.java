package com.mulehunter.backend.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "newtransactions")
public class Transaction {

    @Id
    private String id;

    private String sourceAccount;
    private String targetAccount;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private boolean suspectedFraud;

    private Double riskScore;
    private String verdict;

    public Transaction() {
    }

    public static Transaction from(TransactionRequest request) {
        Transaction tx = new Transaction();
        tx.sourceAccount = request.getSourceAccount();
        tx.targetAccount = request.getTargetAccount();
        if (request.getAmount() == null) {
            tx.amount = BigDecimal.ZERO;
        } else {
            tx.amount = request.getAmount();
        }
        tx.suspectedFraud = false;
        tx.riskScore = 0.0;
        tx.verdict = "PENDING";
        return tx;
    }

    public String getId() {
        return id;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isSuspectedFraud() {
        return suspectedFraud;
    }

    public void setSuspectedFraud(boolean suspectedFraud) {
        this.suspectedFraud = suspectedFraud;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }
}