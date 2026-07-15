package com.edge.browser;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.edge.browser.privacy.PrivacyManager;
import com.edge.browser.tab.SleepingTabManager;
import com.edge.browser.theme.ThemeManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey);
            setupPreferences();
        }

        private void setupPreferences() {
            // General
            SwitchPreferenceCompat startupBoost = findPreference("startup_boost");
            if (startupBoost != null) {
                startupBoost.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    // Toggle startup boost service
                    return true;
                });
            }

            // Sleeping Tabs
            SwitchPreferenceCompat sleepingTabs = findPreference("sleeping_tabs");
            ListPreference sleepTimeout = findPreference("sleep_timeout");

            if (sleepingTabs != null) {
                sleepingTabs.setChecked(SleepingTabManager.getInstance().isEnabled());
                sleepingTabs.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    SleepingTabManager.getInstance().setEnabled(enabled);
                    return true;
                });
            }

            if (sleepTimeout != null) {
                sleepTimeout.setOnPreferenceChangeListener((pref, newValue) -> {
                    String value = (String) newValue;
                    long timeout = Long.parseLong(value) * 1000;
                    SleepingTabManager.getInstance().setSleepTimeout(timeout);
                    return true;
                });
            }

            // Efficiency Mode
            SwitchPreferenceCompat efficiencyMode = findPreference("efficiency_mode");
            if (efficiencyMode != null) {
                efficiencyMode.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    SleepingTabManager.getInstance().setEfficiencyMode(enabled);
                    com.edge.browser.performance.PerformanceManager.getInstance()
                            .setEfficiencyMode(enabled);
                    return true;
                });
            }

            // Tracking Protection
            ListPreference trackingProtection = findPreference("tracking_protection");
            if (trackingProtection != null) {
                trackingProtection.setOnPreferenceChangeListener((pref, newValue) -> {
                    String value = (String) newValue;
                    PrivacyManager.TrackingLevel level = PrivacyManager.TrackingLevel.valueOf(value);
                    PrivacyManager.getInstance(getContext()).applyTrackingProtection(level);
                    return true;
                });
            }

            // HTTPS Only
            SwitchPreferenceCompat httpsOnly = findPreference("https_only");
            if (httpsOnly != null) {
                httpsOnly.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    PrivacyManager.getInstance(getContext()).setHttpsOnly(enabled);
                    return true;
                });
            }

            // Do Not Track
            SwitchPreferenceCompat doNotTrack = findPreference("do_not_track");
            if (doNotTrack != null) {
                doNotTrack.setChecked(PrivacyManager.getInstance(getContext()).isDoNotTrack());
                doNotTrack.setOnPreferenceChangeListener((pref, newValue) -> {
                    PrivacyManager.getInstance(getContext()).setDoNotTrack((Boolean) newValue);
                    return true;
                });
            }

            // Block Popups
            SwitchPreferenceCompat blockPopups = findPreference("block_popups");
            if (blockPopups != null) {
                blockPopups.setChecked(PrivacyManager.getInstance(getContext()).isBlockPopups());
                blockPopups.setOnPreferenceChangeListener((pref, newValue) -> {
                    PrivacyManager.getInstance(getContext()).setBlockPopups((Boolean) newValue);
                    return true;
                });
            }

            // Block Ads
            SwitchPreferenceCompat blockAds = findPreference("block_ads");
            if (blockAds != null) {
                blockAds.setChecked(PrivacyManager.getInstance(getContext()).isBlockAds());
                blockAds.setOnPreferenceChangeListener((pref, newValue) -> {
                    PrivacyManager.getInstance(getContext()).setBlockAds((Boolean) newValue);
                    return true;
                });
            }

            // Auto Clear Cookies
            SwitchPreferenceCompat autoClearCookies = findPreference("auto_clear_cookies");
            if (autoClearCookies != null) {
                autoClearCookies.setOnPreferenceChangeListener((pref, newValue) -> {
                    PrivacyManager.getInstance(getContext()).setAutoClearCookies((Boolean) newValue);
                    return true;
                });
            }

            // Theme
            ListPreference themeMode = findPreference("theme_mode");
            if (themeMode != null) {
                themeMode.setOnPreferenceChangeListener((pref, newValue) -> {
                    String value = (String) newValue;
                    ThemeManager.ThemeMode mode = ThemeManager.ThemeMode.valueOf(value);
                    ThemeManager.getInstance().setCurrentTheme(mode);
                    // Recreate activity to apply theme
                    getActivity().recreate();
                    return true;
                });
            }

            // New Tab Layout
            ListPreference newTabLayout = findPreference("new_tab_layout");
            if (newTabLayout != null) {
                newTabLayout.setOnPreferenceChangeListener((pref, newValue) -> {
                    String value = (String) newValue;
                    ThemeManager.NewTabLayout layout = ThemeManager.NewTabLayout.valueOf(value);
                    ThemeManager.getInstance().setNewTabLayout(layout);
                    return true;
                });
            }

            // Download Parallel
            SwitchPreferenceCompat parallelDownload = findPreference("parallel_download");
            if (parallelDownload != null) {
                parallelDownload.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    // Enable parallel download acceleration
                    return true;
                });
            }

            // Allow Extensions from Other Stores
            SwitchPreferenceCompat otherStoreExt = findPreference("other_store_extensions");
            if (otherStoreExt != null) {
                otherStoreExt.setOnPreferenceChangeListener((pref, newValue) -> {
                    // Toggle extension store compatibility
                    return true;
                });
            }

            // Hardware Acceleration
            SwitchPreferenceCompat hwAccel = findPreference("hardware_acceleration");
            if (hwAccel != null) {
                hwAccel.setOnPreferenceChangeListener((pref, newValue) -> {
                    // Toggle hardware acceleration
                    return true;
                });
            }

            // Experimental Features
            Preference experimental = findPreference("experimental_features");
            if (experimental != null) {
                experimental.setOnPreferenceClickListener(pref -> {
                    // Open experimental features page
                    return true;
                });
            }

            // Clear Browsing Data
            Preference clearData = findPreference("clear_browsing_data");
            if (clearData != null) {
                clearData.setOnPreferenceClickListener(pref -> {
                    // Show clear data dialog
                    return true;
                });
            }

            // Passwords
            Preference passwords = findPreference("passwords");
            if (passwords != null) {
                passwords.setOnPreferenceClickListener(pref -> {
                    // Open password manager
                    return true;
                });
            }

            // Site Permissions
            Preference sitePermissions = findPreference("site_permissions");
            if (sitePermissions != null) {
                sitePermissions.setOnPreferenceClickListener(pref -> {
                    // Open site permissions
                    return true;
                });
            }
        }
    }
}