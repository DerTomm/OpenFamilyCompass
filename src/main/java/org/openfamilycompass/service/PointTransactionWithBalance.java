package org.openfamilycompass.service;

import org.openfamilycompass.model.PointTransaction;

public class PointTransactionWithBalance {
    private final PointTransaction transaction;
    private final int balance;

    public PointTransactionWithBalance(PointTransaction transaction, int balance) {
        this.transaction = transaction;
        this.balance = balance;
    }

    public PointTransaction getTransaction() {
        return transaction;
    }

    public int getBalance() {
        return balance;
    }
}