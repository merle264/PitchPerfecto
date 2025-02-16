package com.example.karaoke;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.InputStreamReader;

public class LicensesActivity extends AppCompatActivity {

    private TextView licenseTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);  // Reference your layout for the Licenses screen

        licenseTextView = findViewById(R.id.licenseTextView);

        // Load licenses from raw resources
        loadLicenses();
    }

    private void loadLicenses() {
        try {
            // Load licenses file from res/raw/ (e.g., license.txt)
            InputStream inputStream = getResources().openRawResource(R.raw.license);
            InputStreamReader reader = new InputStreamReader(inputStream);
            char[] buffer = new char[1024];
            int length;
            StringBuilder stringBuilder = new StringBuilder();

            while ((length = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, length);
            }

            licenseTextView.setText(stringBuilder.toString());  // Set the license content to the TextView
        } catch (Exception e) {
            licenseTextView.setText("Error loading licenses.");  // Error handling
        }
    }
}
