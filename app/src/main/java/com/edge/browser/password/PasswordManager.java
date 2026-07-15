package com.edge.browser.password;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class PasswordManager {

    private static PasswordManager instance;
    private final DatabaseHelper db;

    private PasswordManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized PasswordManager getInstance(Context context) {
        if (instance == null) {
            instance = new PasswordManager(context.getApplicationContext());
        }
        return instance;
    }

    public long savePassword(String domain, String username, String password) {
        return db.addPassword(domain, username, password);
    }

    public List<DatabaseHelper.PasswordEntry> getPassword(String domain) {
        return DatabaseHelper.cursorToPasswords(db.getPasswords(domain));
    }

    public List<DatabaseHelper.PasswordEntry> getAllPasswords() {
        return DatabaseHelper.cursorToPasswords(db.getAllPasswords());
    }

    public void removePassword(long id) {
        db.removePassword(id);
    }
}