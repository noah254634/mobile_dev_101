package com.example.attendance2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView welcomeText = findViewById(R.id.welcomeTextView);
        SharedPreferences preferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        
        // Try getting the global username first (stored during login)
        String username = preferences.getString("global_username", preferences.getString("username", "User"));
        String role = preferences.getString("user_role", "student"); // Default to student
        
        if (username.contains("@")) {
            username = username.split("@")[0];
        }

        welcomeText.setText("Welcome Home, " + username + " (" + role + ")");

        // Role-based UI logic
        if ("lecturer".equals(role)) {
            Intent intent=new Intent(HomeActivity.this,LecturersDashboardActivity.class);
            startActivity(intent);

        } else if("student".equals(role)) {
            Intent intent = new Intent(HomeActivity.this, StudentsDashboardActivity.class);
            startActivity(intent);
            // TODO: Show student-specific buttons (e.g., View My Attendance)
        } else if ("admin".equals(role)) {
            Intent intent = new Intent(HomeActivity.this, AdminDashboard.class);
            startActivity(intent);
        }

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // 1. Sign out from Firebase
            mAuth.signOut();

            // 2. Clear session preferences
            SharedPreferences logoutPrefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = logoutPrefs.edit();
            editor.remove("global_username"); 
            editor.apply();

            // 3. Navigate to Login and clear backstack
            Toast.makeText(HomeActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}