package com.example.attendance2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

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

        EditText resetEmailEditText = findViewById(R.id.resetEmailEditText);
        Button resetPasswordButton = findViewById(R.id.resetPasswordButton);
        Button backToLoginButton = findViewById(R.id.backToLoginButton);

        resetPasswordButton.setOnClickListener(v -> {
            String email = resetEmailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Implement password reset logic
                Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        backToLoginButton.setOnClickListener(v -> finish());
    }
}