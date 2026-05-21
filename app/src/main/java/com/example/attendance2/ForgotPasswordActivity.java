package com.example.attendance2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;



public class ForgotPasswordActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        EditText resetEmailEditText = findViewById(R.id.resetEmailEditText);
        Button resetPasswordButton = findViewById(R.id.resetPasswordButton);
        Button backToLoginButton = findViewById(R.id.backToLoginButton);
        ProgressBar resetProgressBar = findViewById(R.id.resetProgressBar);
        TextView statusMessageTextView = findViewById(R.id.statusMessageTextView);

        resetPasswordButton.setOnClickListener(v -> {
            String email = resetEmailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, R.string.enter_email, Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(ForgotPasswordActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            } else {
                resetProgressBar.setVisibility(View.VISIBLE);
                statusMessageTextView.setVisibility(View.GONE);
                resetPasswordButton.setEnabled(false);

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            resetProgressBar.setVisibility(View.GONE);
                            resetPasswordButton.setEnabled(true);
                            if (task.isSuccessful()) {
                                String successMsg = getString(R.string.recovery_link_sent, email);
                                statusMessageTextView.setText(successMsg);
                                statusMessageTextView.setVisibility(View.VISIBLE);
                                statusMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light, getTheme()));
                                Toast.makeText(ForgotPasswordActivity.this, successMsg, Toast.LENGTH_SHORT).show();
                            } else {
                                Exception e = task.getException();
                                String error;
                                if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                    error = "This email address is not registered.";
                                } else if (e instanceof com.google.firebase.FirebaseNetworkException) {
                                    error = "Network error. Please check your connection.";
                                } else {
                                    error = e != null ? e.getMessage() : "Failed to send recovery email";
                                }

                                statusMessageTextView.setText(error);
                                statusMessageTextView.setVisibility(View.VISIBLE);
                                statusMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light, getTheme()));
                                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        backToLoginButton.setOnClickListener(v -> finish());
    }
}