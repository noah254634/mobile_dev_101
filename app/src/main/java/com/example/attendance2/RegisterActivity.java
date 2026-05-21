package com.example.attendance2;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Spinner roleSpinner;
    private TextInputEditText emailEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private TextInputEditText regNumberEditText, departmentEditText;
    private View regNumberLayout, departmentLayout;
    private MaterialButton registerButton, loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        emailEditText = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        regNumberEditText = findViewById(R.id.regNumberEditText);
        departmentEditText = findViewById(R.id.departmentEditText);
        regNumberLayout = findViewById(R.id.regNumberLayout);
        departmentLayout = findViewById(R.id.departmentLayout);
        
        roleSpinner = findViewById(R.id.roleSpinner);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);

        // Setup Role Spinner
        String[] roles = {
                getString(R.string.role_student),
                getString(R.string.role_lecturer),
                getString(R.string.role_admin)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String role = roles[position].toLowerCase();
                if (role.contains("student")) {
                    regNumberLayout.setVisibility(View.VISIBLE);
                    departmentLayout.setVisibility(View.VISIBLE);
                } else if (role.contains("lecturer")) {
                    regNumberLayout.setVisibility(View.GONE);
                    departmentLayout.setVisibility(View.VISIBLE);
                } else {
                    regNumberLayout.setVisibility(View.GONE);
                    departmentLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        loginButton.setOnClickListener(v -> finish());

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
            String username = usernameEditText.getText() != null ? usernameEditText.getText().toString().trim() : "";
            String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
            String confirmPassword = confirmPasswordEditText.getText() != null ? confirmPasswordEditText.getText().toString().trim() : "";
            String regNumber = regNumberEditText.getText() != null ? regNumberEditText.getText().toString().trim() : "";
            String department = departmentEditText.getText() != null ? departmentEditText.getText().toString().trim() : "";
            String selectedRole = roleSpinner.getSelectedItem().toString().toLowerCase();
            String deviceId = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(RegisterActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, R.string.passwords_dont_match, Toast.LENGTH_SHORT).show();
            } else {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                if (mAuth.getCurrentUser() != null) {
                                    String userId = mAuth.getCurrentUser().getUid();
                                    Users user;
                                    
                                    if (selectedRole.contains("student")) {
                                        user = new Student(userId, username, email, regNumber, department, deviceId, getDeviceName());
                                    } else if (selectedRole.contains("lecturer")) {
                                        user = new Lecturer(userId, username, email, department);
                                    } else {
                                        user = new Admin(userId, username, email);
                                    }

                                    mDatabase.child("Users").child(userId).setValue(user)
                                            .addOnCompleteListener(dbTask -> {
                                                if (dbTask.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this, R.string.registration_successful, Toast.LENGTH_SHORT).show();
                                                    finish();
                                                }
                                            });
                                }
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                Toast.makeText(RegisterActivity.this, "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}