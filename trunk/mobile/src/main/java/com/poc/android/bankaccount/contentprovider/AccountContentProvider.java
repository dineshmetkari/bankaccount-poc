package com.poc.android.bankaccount.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class AccountContentProvider extends ContentProvider {
    // used for the UriMatcher
    private static final int ACCOUNTS = 10;
    private static final int ACCOUNT_ID = 20;

    private static final String ACCOUNTS_PATH = "locations";

    public static final String AUTHORITY = "com.poc.android.bankaccount.datasync.provider";
    @SuppressWarnings("UnusedDeclaration")
    public static final Uri LOCATIONS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ACCOUNTS_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH, ACCOUNTS);
        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH + "/#", ACCOUNT_ID);
    }

    public AccountContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
