package com.poc.android.bankaccount.library.model;

import android.database.Cursor;

import com.google.gson.annotations.Expose;
import com.poc.android.bankaccount.library.contentprovider.AccountContentProvider;

@SuppressWarnings("UnusedDeclaration")
public class BankAccount {
    @Expose
    private String name;
    @Expose
    private String id;
    @Expose
    private int balance;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", balance=" + balance +
                '}';
    }

    public static BankAccount cursorToBankAccount(Cursor cursor) {
        BankAccount bankAccount = new BankAccount();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            bankAccount.setName(cursor.getString(cursor.getColumnIndex(AccountContentProvider.ACCOUNT_FIELD_NAME)));
            bankAccount.setId(cursor.getString(cursor.getColumnIndex(AccountContentProvider.ACCOUNT_FIELD_ID)));
            bankAccount.setBalance(cursor.getInt(cursor.getColumnIndex(AccountContentProvider.ACCOUNT_FIELD_BALANCE)));
        }

        return bankAccount;
    }
}
