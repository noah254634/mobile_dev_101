package com.example.attendance2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialCheckBox rememberMeCheckbox;
    private MaterialButton loginButton, registerButton, forgotPasswordButton;

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            fetchUserDataAndNavigate(mAuth.getCurrentUser().getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        emailEditText = findViewById(R.id.loginEmailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);

        SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        if (preferences.getBoolean("rememberMe", false)) {
            emailEditText.setText(preferences.getString("email", ""));
            passwordEditText.setText(preferences.getString("password", ""));
            rememberMeCheckbox.setChecked(true);
        }

        loginButton.setOnClickListener(v -> handleLogin(preferences));

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin(SharedPreferences preferences) {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
        boolean rememberMe = rememberMeCheckbox.isChecked();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
        } else if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Auth successful, fetching user data...");
                            SharedPreferences.Editor editor = preferences.edit();
                            if (rememberMe) {
                                editor.putString("email", email);
                                editor.putString("password", password);
                                editor.putBoolean("rememberMe", true);
                            } else {
                                editor.clear();
                            }
                            editor.apply();

                            if (mAuth.getCurrentUser() != null) {
                                fetchUserDataAndNavigate(mAuth.getCurrentUser().getUid());
                            } else {
                                Log.e(TAG, "Auth successful but current user is null!");
                            }
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Log.e(TAG, "Login failed: " + error);
                            Toast.makeText(MainActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void fetchUserDataAndNavigate(String userId) {
        Log.d(TAG, "Fetching data for UID: " + userId);
        mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "No user data found in Realtime Database for UID: " + userId);
                    Toast.makeText(MainActivity.this, "User record missing", Toast.LENGTH_LONG).show();
                    return;
                }

                Users user = snapshot.getValue(Users.class);
                if (user != null) {
                    Log.d(TAG, "User data found. Role: " + user.role + ", Username: " + user.username);
                    SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("user_role", user.role);
                    editor.putString("global_username", user.username);
                    editor.apply();

                    // Update last login
                    mDatabase.child("Users").child(userId).child("lastLogin").setValue(System.currentTimeMillis());

                    Intent intent;
                    String role = user.role != null ? user.role.toLowerCase().trim() : "";
                    
                    if (role.contains("admin")) {
                        intent = new Intent(MainActivity.this, AdminDashboard.class);
                    } else if (role.contains("lecturer")) {
                        intent = new Intent(MainActivity.this, LecturersDashboardActivity.class);
                    } else if (role.contains("student")) {
                        intent = new Intent(MainActivity.this, StudentsDashboardActivity.class);
                    } else {
                        Log.w(TAG, "Unknown role: " + user.role + ". Redirecting to Home.");
                        intent = new Intent(MainActivity.this, HomeActivity.class);
                    }
                    
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "Failed to parse Users object");
                    startActivity(intentToHome());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                startActivity(intentToHome());
            }
        });
    }

    private Intent intentToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}