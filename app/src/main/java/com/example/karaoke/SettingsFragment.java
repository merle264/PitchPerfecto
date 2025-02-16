package com.example.karaoke;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load preferences from the XML file
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Find the "Dark Mode" switch preference
        SwitchPreferenceCompat darkModeSwitch = findPreference("dark_mode");

        // Find the "Licenses" preference and set the click listener
        Preference licensesPreference = findPreference("license");
        if (licensesPreference != null) {
            licensesPreference.setOnPreferenceClickListener(preference -> {
                openLicenses();  // Call openLicenses when the preference is clicked
                return true;  // Return true to indicate the click event was handled
            });
        }
    }

    // Method to open LicensesActivity
    private void openLicenses() {
        // Open LicensesActivity when the "Licenses" preference is clicked
        Intent intent = new Intent(getActivity(), LicensesActivity.class);
        startActivity(intent);
    }
}
