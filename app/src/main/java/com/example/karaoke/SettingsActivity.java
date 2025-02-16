package com.example.karaoke;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if dark mode is enabled in shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false); // Default to false (light mode)

        // Apply dark mode based on the saved preference
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);  // Dark mode
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);   // Light mode
        }

        setContentView(R.layout.activity_settings);  // Reference your settings layout file

        // Load the fragment that holds the settings UI
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment()) // Replace with the SettingsFragment
                    .commit();
        }
    }

    // Method triggered when the "Licenses" preference is clicked
    public boolean openLicenses(androidx.preference.Preference preference) {
        // Open LicensesActivity when the "Licenses" preference is clicked
        Intent intent = new Intent(this, LicensesActivity.class);
        startActivity(intent);
        return true; // Return true to indicate that the click event was handled
    }
}
