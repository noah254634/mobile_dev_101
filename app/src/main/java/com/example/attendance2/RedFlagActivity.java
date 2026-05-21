package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RedFlagActivity extends AppCompatActivity {

    private TextInputEditText studentRegEditText, courseCodeEditText, incidentDescriptionEditText;
    private AutoCompleteTextView incidentTypeAutoComplete;
    private RadioGroup severityRadioGroup;
    private MaterialButton submitRedFlagButton;
    private BottomNavigationView bottomNavigation;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_red_flag);
        
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        studentRegEditText = findViewById(R.id.studentRegEditText);
        courseCodeEditText = findViewById(R.id.courseCodeEditText);
        incidentDescriptionEditText = findViewById(R.id.incidentDescriptionEditText);
        incidentTypeAutoComplete = findViewById(R.id.incidentTypeAutoComplete);
        severityRadioGroup = findViewById(R.id.severityRadioGroup);
        submitRedFlagButton = findViewById(R.id.submitRedFlagButton);
        bottomNavigation = findViewById(R.id.lecturerBottomNavigation);

        // Setup Dropdown for Incident Types
        String[] incidentTypes = {"Device Fingerprint Mismatch", "Location Spoofing Detected", "Unauthorized Proxy Attempt", "Repeated Late Arrival", "Manual Identity Verification Failed"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, incidentTypes);
        incidentTypeAutoComplete.setAdapter(adapter);

        submitRedFlagButton.setOnClickListener(v -> submitRedFlag());

        if (bottomNavigation != null) {
            bottomNavigation.getMenu().clear();
            bottomNavigation.inflateMenu(R.menu.menu_lecturer_nav);
            // This screen doesn't have a direct nav item, but we'll highlight Sessions or Home
            bottomNavigation.setSelectedItemId(R.id.nav_reports); 
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_sessions) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    handleLogout();
                    return true;
                }
                return false;
            });
        }
    }

    private void submitRedFlag() {
        String studentReg = studentRegEditText.getText().toString().trim();
        String courseCode = courseCodeEditText.getText().toString().trim();
        String description = incidentDescriptionEditText.getText().toString().trim();
        String type = incidentTypeAutoComplete.getText().toString();
        
        int selectedSeverityId = severityRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedSeverity = findViewById(selectedSeverityId);
        String severity = selectedSeverity != null ? selectedSeverity.getText().toString() : "Critical";

        if (studentReg.isEmpty() || courseCode.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String flagId = mDatabase.child("RedFlags").push().getKey();
        Map<String, Object> flagData = new HashMap<>();
        flagId = flagId != null ? flagId : String.valueOf(System.currentTimeMillis());
        
        flagData.put("flagId", flagId);
        flagData.put("studentReg", studentReg);
        flagData.put("courseCode", courseCode);
        flagData.put("description", description);
        flagData.put("type", type);
        flagData.put("severity", severity);
        flagData.put("timestamp", System.currentTimeMillis());
        flagData.put("reporterId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        mDatabase.child("RedFlags").child(flagId).setValue(flagData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Red Flag reported to administration", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to submit flag: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}