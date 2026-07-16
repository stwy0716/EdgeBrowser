package com.edge.browser.autofill;

import android.content.Context;
import android.content.SharedPreferences;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AutofillManager {

    private static final String TAG = "AutofillManager";
    private static final String PREFS_NAME = "edge_autofill_prefs";
    private static final String KEY_ADDRESSES = "autofill_addresses";
    private static final String KEY_PAYMENTS = "autofill_payments";
    private static final String KEY_LOGINS = "autofill_logins";

    private static AutofillManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private List<AddressEntry> addressCache;
    private List<PaymentEntry> paymentCache;
    private List<LoginEntry> loginCache;

    public static class AddressEntry {
        public String name;
        public String street;
        public String city;
        public String state;
        public String zip;
        public String country;
        public String phone;
        public String email;

        public AddressEntry() {}

        public AddressEntry(String name, String street, String city, String state,
                            String zip, String country, String phone, String email) {
            this.name = name;
            this.street = street;
            this.city = city;
            this.state = state;
            this.zip = zip;
            this.country = country;
            this.phone = phone;
            this.email = email;
        }
    }

    public static class PaymentEntry {
        public String cardName;
        public String lastFourDigits;
        public int expiryMonth;
        public int expiryYear;

        public PaymentEntry() {}

        public PaymentEntry(String cardName, String lastFourDigits, int expiryMonth, int expiryYear) {
            this.cardName = cardName;
            this.lastFourDigits = lastFourDigits;
            this.expiryMonth = expiryMonth;
            this.expiryYear = expiryYear;
        }
    }

    public static class LoginEntry {
        public String domain;
        public String username;
        public String encryptedPassword;

        public LoginEntry() {}

        public LoginEntry(String domain, String username, String encryptedPassword) {
            this.domain = domain;
            this.username = username;
            this.encryptedPassword = encryptedPassword;
        }
    }

    private AutofillManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCache();
        BrowserLogger.getInstance().d(TAG, LogCategory.PRIVACY, "AutofillManager initialized");
    }

    public static synchronized AutofillManager getInstance(Context context) {
        if (instance == null) {
            instance = new AutofillManager(context.getApplicationContext());
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void loadCache() {
        addressCache = loadList(KEY_ADDRESSES, new TypeToken<ArrayList<AddressEntry>>(){}.getType());
        paymentCache = loadList(KEY_PAYMENTS, new TypeToken<ArrayList<PaymentEntry>>(){}.getType());
        loginCache = loadList(KEY_LOGINS, new TypeToken<ArrayList<LoginEntry>>(){}.getType());
    }

    private <T> List<T> loadList(String key, Type type) {
        String json = prefs.getString(key, null);
        if (json != null) {
            try {
                return gson.fromJson(json, type);
            } catch (Exception e) {
                BrowserLogger.getInstance().e(TAG, LogCategory.PRIVACY, "Failed to load " + key, e);
            }
        }
        return new ArrayList<>();
    }

    private void saveList(String key, List<?> list) {
        String json = gson.toJson(list);
        prefs.edit().putString(key, json).apply();
    }

    public void saveAddress(AddressEntry entry) {
        if (entry == null) return;
        addressCache.add(entry);
        saveList(KEY_ADDRESSES, addressCache);
        BrowserLogger.getInstance().d(TAG, LogCategory.PRIVACY, "Address saved: " + entry.name);
    }

    public List<AddressEntry> getAddresses() {
        return new ArrayList<>(addressCache);
    }

    public void savePayment(PaymentEntry entry) {
        if (entry == null) return;
        paymentCache.add(entry);
        saveList(KEY_PAYMENTS, paymentCache);
        BrowserLogger.getInstance().d(TAG, LogCategory.PRIVACY, "Payment saved: " + entry.cardName);
    }

    public List<PaymentEntry> getPayments() {
        return new ArrayList<>(paymentCache);
    }

    public void saveLogin(String domain, String username, String password) {
        if (domain == null || username == null || password == null) return;
        LoginEntry entry = new LoginEntry(domain, username, password);
        LoginEntry existing = null;
        for (LoginEntry l : loginCache) {
            if (l.domain != null && l.domain.equals(domain)) {
                existing = l;
                break;
            }
        }
        if (existing != null) {
            loginCache.remove(existing);
        }
        loginCache.add(entry);
        saveList(KEY_LOGINS, loginCache);
        BrowserLogger.getInstance().d(TAG, LogCategory.PRIVACY, "Login saved for domain: " + domain);
    }

    public LoginEntry getLoginForDomain(String domain) {
        if (domain == null) return null;
        for (LoginEntry entry : loginCache) {
            if (domain.equals(entry.domain)) {
                return entry;
            }
        }
        return null;
    }

    public void removeLogin(String domain) {
        if (domain == null) return;
        LoginEntry toRemove = null;
        for (LoginEntry entry : loginCache) {
            if (domain.equals(entry.domain)) {
                toRemove = entry;
                break;
            }
        }
        if (toRemove != null) {
            loginCache.remove(toRemove);
            saveList(KEY_LOGINS, loginCache);
            BrowserLogger.getInstance().d(TAG, LogCategory.PRIVACY, "Login removed for domain: " + domain);
        }
    }

    public void clearAll() {
        addressCache.clear();
        paymentCache.clear();
        loginCache.clear();
        prefs.edit().clear().apply();
        BrowserLogger.getInstance().i(TAG, LogCategory.PRIVACY, "All autofill data cleared");
    }
}