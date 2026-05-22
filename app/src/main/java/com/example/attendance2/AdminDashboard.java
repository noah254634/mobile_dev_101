package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class AdminDashboard extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private TextView adminNameTextView, totalUsersCount, activeSessionsCount;
    private View securityAlertsCard;
    private TextView securityAlertSummary;
    private EditText targetUserIdentifierEditText;
    private Spinner targetRoleDropdown;
    private MaterialButton applyRoleChangeButton, manageCoursesButton, logoutButton;
    private MaterialButton exportReportImageButton, monitorUsersButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_admin_dashboard);

            mAuth = FirebaseAuth.getInstance();
            // Use default instance if possible, fallback to URL
            try {
                mDatabase = FirebaseDatabase.getInstance().getReference();
            } catch (Exception e) {
                mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();
            }

            // Initialize Views
            adminNameTextView = findViewById(R.id.adminNameTextView);
            totalUsersCount = findViewById(R.id.totalUsersCount);
            activeSessionsCount = findViewById(R.id.activeSessionsCount);
            securityAlertsCard = findViewById(R.id.securityAlertsCard);
            securityAlertSummary = findViewById(R.id.securityAlertSummary);
            targetUserIdentifierEditText = findViewById(R.id.targetUserIdentifierEditText);
            targetRoleDropdown = findViewById(R.id.targetRoleDropdown);
            applyRoleChangeButton = findViewById(R.id.applyRoleChangeButton);
            manageCoursesButton = findViewById(R.id.manageCoursesButton);
            logoutButton = findViewById(R.id.logoutButton);
            exportReportImageButton = findViewById(R.id.exportReportImageButton);
            monitorUsersButton = findViewById(R.id.monitorUsersButton);

            loadDashboardData();
            loadSecurityAlerts();
            checkAccountSecurity();

            securityAlertsCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserMonitoringActivity.class);
                intent.putExtra("filter_flagged", true);
                startActivity(intent);
            });

            BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
                bottomNavigation.setOnItemSelectedListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.nav_home) {
                        return true;
                    } else if (itemId == R.id.nav_management) {
                        Intent intent = new Intent(AdminDashboard.this, ManageCoursesActivity.class);
                        startActivity(intent);
                        return true;
                    } else if (itemId == R.id.nav_monitoring) {
                        startActivity(new Intent(this, UserMonitoringActivity.class));
                        return true;
                    } else if (itemId == R.id.nav_logout) {
                        handleLogout();
                        return true;
                    } else if (itemId == R.id.nav_gps) {
                        startActivity(new Intent(this, VenueActivity.class));
                        return true;
                    }
                    return false;
                });
            }

            // Setup Role Spinner
            String[] roles = {
                    getString(R.string.role_student),
                    getString(R.string.role_lecturer),
                    getString(R.string.role_admin)
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (targetRoleDropdown != null) {
                targetRoleDropdown.setAdapter(adapter);
            }

            loadDashboardData();

            // Basic Click Listeners
            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> handleLogout());
            }

            if (monitorUsersButton != null) {
                monitorUsersButton.setOnClickListener(v -> {
                    Intent intent = new Intent(AdminDashboard.this, UserMonitoringActivity.class);
                    startActivity(intent);
                });
            }

            if (applyRoleChangeButton != null) {
                applyRoleChangeButton.setOnClickListener(v -> {
                    String identifier = targetUserIdentifierEditText.getText().toString().trim();
                    if (targetRoleDropdown.getSelectedItem() == null) return;
                    String selectedRole = targetRoleDropdown.getSelectedItem().toString().toLowerCase();

                    if (identifier.isEmpty()) {
                        targetUserIdentifierEditText.setError(getString(R.string.enter_user_id_or_email));
                    } else {
                        updateUserRole(identifier, selectedRole);
                    }
                });
            }

            if (manageCoursesButton != null) {
                manageCoursesButton.setOnClickListener(v -> {
                    Intent intent=new Intent(AdminDashboard.this,ManageCoursesActivity.class);
                    startActivity(intent);
                });
            }

            if (exportReportImageButton != null) {
                exportReportImageButton.setOnClickListener(v -> {
                    Toast.makeText(this, R.string.generating_report, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading dashboard", Toast.LENGTH_LONG).show();
        }
    }

    private void showCreateCourseDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_create_course, null);
        EditText editName = dialogView.findViewById(R.id.editCourseName);
        EditText editCode = dialogView.findViewById(R.id.editCourseCode);
        Spinner venueSpinner = dialogView.findViewById(R.id.venueSpinner);
        Spinner lecturerSpinner = dialogView.findViewById(R.id.lecturerSpinner);

        // Fetch data
        java.util.List<String> lecturerEmails = new java.util.ArrayList<>();
        java.util.List<String> lecturerNames = new java.util.ArrayList<>();
        java.util.List<String> venueCodes = new java.util.ArrayList<>();
        java.util.List<String> venueNames = new java.util.ArrayList<>();
        
        mDatabase.child("Users").orderByChild("role").equalTo("lecturer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot lecturerSnap : snapshot.getChildren()) {
                    Lecturer lecturer = lecturerSnap.getValue(Lecturer.class);
                    if (lecturer != null) {
                        lecturerEmails.add(lecturer.email);
                        lecturerNames.add(lecturer.username + " (" + lecturer.email + ")");
                    }
                }
                ArrayAdapter<String> lecturerAdapter = new ArrayAdapter<>(AdminDashboard.this, android.R.layout.simple_spinner_item, lecturerNames);
                lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lecturerSpinner.setAdapter(lecturerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("Venues").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot venueSnap : snapshot.getChildren()) {
                    VenueGps venue = venueSnap.getValue(VenueGps.class);
                    if (venue != null) {
                        venueCodes.add(venue.venueCode);
                        venueNames.add(venue.venueName + " (" + venue.venueCode + ")");
                    }
                }
                ArrayAdapter<String> venueAdapter = new ArrayAdapter<>(AdminDashboard.this, android.R.layout.simple_spinner_item, venueNames);
                venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                venueSpinner.setAdapter(venueAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_course_title)
                .setView(dialogView)
                .setPositiveButton(R.string.create_button, (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String code = editCode.getText().toString().trim();
                    int lPos = lecturerSpinner.getSelectedItemPosition();
                    int vPos = venueSpinner.getSelectedItemPosition();

                    if (!name.isEmpty() && !code.isEmpty() && lPos != -1 && vPos != -1) {
                        String selectedLecturerEmail = lecturerEmails.get(lPos);
                        String selectedLecturerName = lecturerNames.get(lPos).split(" \\(")[0];
                        String selectedVenueCode = venueCodes.get(vPos);
                        createNewCourse(name, code, selectedVenueCode, selectedLecturerName, selectedLecturerEmail);
                    } else {
                        Toast.makeText(this, "Please fill all fields and select data", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createNewCourse(String name, String code, String venue, String lecturerName, String lecturerEmail) {
        String courseId = mDatabase.child("Courses").push().getKey();
        
        Course newCourse = new Course(courseId, name, code, lecturerName, lecturerEmail, venue);
        
        if (courseId != null) {
            mDatabase.child("Courses").child(courseId).setValue(newCourse)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.course_created_success, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to create course", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() != null && mDatabase != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Admin admin = snapshot.getValue(Admin.class);
                        if (admin != null && admin.getUsername() != null && adminNameTextView != null) {
                            adminNameTextView.setText(getString(R.string.welcome_admin, admin.getUsername()));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long count = snapshot.getChildrenCount();
                    if (totalUsersCount != null) {
                        totalUsersCount.setText(String.valueOf(count));
                    }
                    if (activeSessionsCount != null) {
                        activeSessionsCount.setText(count + " Live");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void loadSecurityAlerts() {
        mDatabase.child("RedFlags").orderByChild("reporterId").equalTo("SYSTEM_AUTO").limitToLast(1)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        securityAlertsCard.setVisibility(View.VISIBLE);
                        for (DataSnapshot alertSnap : snapshot.getChildren()) {
                            Map<String, Object> flag = (Map<String, Object>) alertSnap.getValue();
                            if (flag != null) {
                                String desc = (String) flag.get("description");
                                securityAlertSummary.setText(desc);
                            }
                        }
                    } else {
                        securityAlertsCard.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void updateUserRole(String identifier, String newRole) {
        if (mDatabase == null) return;
        
        // Sanitize path or use query for email
        if (!identifier.contains(".") && !identifier.contains("@") && !identifier.contains("#") && !identifier.contains("$") && !identifier.contains("[") && !identifier.contains("]")) {
            mDatabase.child("Users").child(identifier).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        mDatabase.child("Users").child(identifier).child("role").setValue(newRole)
                                .addOnSuccessListener(aVoid -> Toast.makeText(AdminDashboard.this, R.string.role_updated, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(AdminDashboard.this, R.string.failed_to_update_role, Toast.LENGTH_SHORT).show());
                    } else {
                        searchByEmailAndUpdate(identifier, newRole);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            searchByEmailAndUpdate(identifier, newRole);
        }
    }

    private void searchByEmailAndUpdate(String email, String newRole) {
        if (mDatabase == null) return;
        mDatabase.child("Users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userSnapshot.getRef().child("role").setValue(newRole)
                                .addOnSuccessListener(aVoid -> Toast.makeText(AdminDashboard.this, R.string.role_updated, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(AdminDashboard.this, R.string.failed_to_update_role, Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(AdminDashboard.this, R.string.user_not_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleLogout() {
        mAuth.signOut();
        Toast.makeText(AdminDashboard.this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboard.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAccountSecurity() {
        if (mAuth.getCurrentUser() == null) return;
        
        mDatabase.child("Users").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Admin admin = snapshot.getValue(Admin.class);
                if (admin != null && admin.isFlagged) {
                    showFlaggedAccountDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showFlaggedAccountDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Restricted")
                .setMessage("System Access Denied: This administrator account has been flagged for security review. High-level operations are disabled.")
                .setPositiveButton("Logout", (dialog, which) -> handleLogout())
                .setCancelable(false)
                .show();
    }
}
