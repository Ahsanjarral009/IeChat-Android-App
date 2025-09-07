package com.example.iechat;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "chat_session";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PRIVATE = "private_key"; // PEM formatted

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save logged-in username
    public void saveUser(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    // Get current logged-in user
    public String getUser() {
        return pref.getString(KEY_USERNAME, null);
    }

    // Clear session (logout)
    public void clearUser() {
        editor.clear();
        editor.apply();
    }

    // Save private key PEM
    public void savePrivateKey(String privateKeyPem) {
        editor.putString(KEY_PRIVATE, privateKeyPem);
        editor.apply();
    }

    // Get private key PEM
    public String getPrivateKey() {
        return pref.getString(KEY_PRIVATE, null);
    }
}
