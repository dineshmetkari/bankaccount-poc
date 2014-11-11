package com.poc.android.bankaccount.library.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.poc.android.bankaccount.library.model.BankAccount;

import java.util.Arrays;

public class AccountContentProvider extends ContentProvider {
    private static final String TAG = "AccountContentProvider";

    // used for the UriMatcher
    private static final int ACCOUNTS = 10;
    private static final int ACCOUNT_ID = 20;

    private static final String ACCOUNTS_PATH = "accounts";

    public static final String AUTHORITY = "com.poc.android.bankaccount.datasync.provider";
    @SuppressWarnings("UnusedDeclaration")
    public static final Uri ACCOUNTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ACCOUNTS_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH, ACCOUNTS);
        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH + "/#", ACCOUNT_ID);
    }

    public static final String ACCOUNT_FIELD_NAME = "name";
    public static final String ACCOUNT_FIELD_ID = "id";
    public static final String ACCOUNT_FIELD_BALANCE = "balance";

    public static final String[] ACCOUNT_ALL_FIELDS = {
            ACCOUNT_FIELD_NAME,
            ACCOUNT_FIELD_ID,
            ACCOUNT_FIELD_BALANCE
    };

    private BankAccount bankAccount;

    public AccountContentProvider() { }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        bankAccount = null;
        return 1;
    }

    @Override
    public String getType(Uri uri) {
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "update(" + uri + ", " + values + ")");

        makeBankAccount(values);

        getContext().getContentResolver().notifyChange(ACCOUNTS_CONTENT_URI, null);

        return Uri.parse(uri.toString() + "#" + bankAccount.getId());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query(" + uri + ", " + Arrays.toString(projection) + ", " + selection + ", " + Arrays.toString(selectionArgs) + ", " + sortOrder + ")");
        MatrixCursor cursor = new MatrixCursor(ACCOUNT_ALL_FIELDS);

        if (bankAccount != null) {
            cursor.addRow(new Object[]{bankAccount.getName(), bankAccount.getId(), bankAccount.getBalance()});
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "update(" + uri + ", " + values + ", " + selection + ", " + Arrays.toString(selectionArgs) + ")");

        makeBankAccount(values);

        getContext().getContentResolver().notifyChange(ACCOUNTS_CONTENT_URI, null);

        return 1;
    }

    private void makeBankAccount(ContentValues values) {
        bankAccount = new BankAccount();
        bankAccount.setBalance(((Number) values.get(ACCOUNT_FIELD_BALANCE)).intValue());
        bankAccount.setId((String) values.get(ACCOUNT_FIELD_ID));
        bankAccount.setName((String) values.get(ACCOUNT_FIELD_NAME));
    }
}
