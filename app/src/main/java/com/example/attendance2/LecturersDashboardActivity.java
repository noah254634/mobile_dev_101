package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LecturersDashboardActivity extends AppCompatActivity {

    private static final String TAG = "LecturerDashboard";
    private TextView lecturerNameTextView, coursesCountTextView, upcomingCountTextView, pendingCountTextView;
    private TextView lecturerDeptTextView, noRedFlagsTextView;
    private Spinner courseSpinner;
    private MaterialButton startAttendanceButton, viewReportsButton, logoutButton, redFlagButton;
    private RecyclerView redFlagsRecyclerView;
    private RedFlagAdapter redFlagAdapter;
    private List<RedFlag> redFlagList = new ArrayList<>();
    private List<Course> assignedCoursesList = new ArrayList<>();
    private List<String> courseSpinnerLabels = new ArrayList<>();
    private ArrayAdapter<String> courseSpinnerAdapter;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private AlertDialog securityDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_lecturers_dashboard);

            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

            // Initialize Views
            lecturerNameTextView = findViewById(R.id.lecturerNameTextView);
            lecturerDeptTextView = findViewById(R.id.lecturerDeptTextView);
            coursesCountTextView = findViewById(R.id.coursesCountTextView);
            upcomingCountTextView = findViewById(R.id.upcomingCountTextView);
            pendingCountTextView = findViewById(R.id.pendingCountTextView);
            courseSpinner = findViewById(R.id.courseSpinner);
            startAttendanceButton = findViewById(R.id.startAttendanceButton);
            viewReportsButton = findViewById(R.id.viewReportsButton);
            logoutButton = findViewById(R.id.logoutButton);
            redFlagButton = findViewById(R.id.redFlagButton);
            
            redFlagsRecyclerView = findViewById(R.id.redFlagsRecyclerView);
            noRedFlagsTextView = findViewById(R.id.noRedFlagsTextView);

            redFlagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            redFlagAdapter = new RedFlagAdapter(redFlagList);
            redFlagsRecyclerView.setAdapter(redFlagAdapter);

            courseSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseSpinnerLabels);
            courseSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            courseSpinner.setAdapter(courseSpinnerAdapter);

            setupNavigation();

            checkAccountSecurity();
            loadDashboardData();
            loadRecentRedFlags();
            loadAssignedCourses();
            loadActiveSessionsCount();

            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> handleLogout());
            }

            if (startAttendanceButton != null) {
                startAttendanceButton.setOnClickListener(v -> {
                    int pos = courseSpinner.getSelectedItemPosition();
                    if (pos != -1 && pos < assignedCoursesList.size()) {
                        Course selectedCourse = assignedCoursesList.get(pos);
                        showSessionDurationDialog(selectedCourse);
                    } else {
                        Toast.makeText(this, "Please select a valid course", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (viewReportsButton != null) {
                viewReportsButton.setOnClickListener(v -> {
                    Toast.makeText(this, R.string.opening_database_allocation, Toast.LENGTH_SHORT).show();
                });
            }

            if (redFlagButton != null) {
                redFlagButton.setOnClickListener(v -> {
                    startActivity(new Intent(this, RedFlagActivity.class));
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_sessions) {
                    startActivity(new Intent(this, LecturerSessionsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    Toast.makeText(this, "Reports coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    handleLogout();
                    return true;
                }
                return false;
            });
        }
    }

    private void checkAccountSecurity() {
        if (mAuth.getCurrentUser() == null) return;
        
        mDatabase.child("Users").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                if (user != null && user.isFlagged) {
                    showFlaggedAccountDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showFlaggedAccountDialog() {
        if (securityDialog != null && securityDialog.isShowing()) return;

        securityDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Restricted")
                .setMessage("Security Alert: This account has been flagged for system violations. Access to the Lecturer Control Center is disabled. Please contact System Administration.")
                .setPositiveButton("Logout", (dialog, which) -> handleLogout())
                .setCancelable(false)
                .show();
    }

    private void loadAssignedCourses() {
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            if (email == null) return;

            mDatabase.child("Courses").orderByChild("lecturerEmail").equalTo(email)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        assignedCoursesList.clear();
                        courseSpinnerLabels.clear();
                        for (DataSnapshot courseSnap : snapshot.getChildren()) {
                            Course course = courseSnap.getValue(Course.class);
                            if (course != null) {
                                assignedCoursesList.add(course);
                                courseSpinnerLabels.add(course.courseCode + " - " + course.courseName);
                            }
                        }
                        courseSpinnerAdapter.notifyDataSetChanged();
                        coursesCountTextView.setText(String.valueOf(assignedCoursesList.size()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    private void showSessionDurationDialog(Course course) {
        String[] durations = {"15 Minutes", "30 Minutes", "1 Hour", "2 Hours", "3 Hours"};
        int[] minutes = {15, 30, 60, 120, 180};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Session Duration")
                .setItems(durations, (dialog, which) -> {
                    startNewSession(course, minutes[which]);
                })
                .show();
    }

    private void startNewSession(Course course, int durationMinutes) {
        if (course.venue == null || course.venue.isEmpty()) {
            Toast.makeText(this, "Error: Course has no venue assigned", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch venue details and then show confirmation
        mDatabase.child("Venues").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                VenueGps foundVenue = null;
                
                // 1. Try lookup by key (venueCode)
                if (snapshot.hasChild(course.venue)) {
                    foundVenue = snapshot.child(course.venue).getValue(VenueGps.class);
                }
                
                // 2. Try search by venueName or venueCode inside objects if key lookup failed
                if (foundVenue == null) {
                    for (DataSnapshot venueSnap : snapshot.getChildren()) {
                        VenueGps v = venueSnap.getValue(VenueGps.class);
                        if (v != null && (course.venue.equalsIgnoreCase(v.venueCode) || course.venue.equalsIgnoreCase(v.venueName))) {
                            foundVenue = v;
                            break;
                        }
                    }
                }

                if (foundVenue != null) {
                    final VenueGps finalVenue = foundVenue;
                    new androidx.appcompat.app.AlertDialog.Builder(LecturersDashboardActivity.this)
                            .setTitle("Confirm Session Details")
                            .setMessage(String.format(Locale.getDefault(), 
                                    "Course: %s\nVenue: %s\nCoordinates: %.5f, %.5f\nRadius: %dm\nDuration: %d Minutes",
                                    course.courseCode, finalVenue.venueName, finalVenue.latitude, finalVenue.longitude, finalVenue.radius, durationMinutes))
                            .setPositiveButton("Start Session", (dialog, which) -> {
                                createSessionWithVenue(course, durationMinutes, finalVenue);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    String errorMsg = "Venue '" + course.venue + "' GPS is not configured. Available venues: ";
                    for (DataSnapshot vs : snapshot.getChildren()) {
                        VenueGps vg = vs.getValue(VenueGps.class);
                        if (vg != null) errorMsg += vg.venueCode + ", ";
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(LecturersDashboardActivity.this)
                            .setTitle("Configuration Error")
                            .setMessage(errorMsg)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LecturersDashboardActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createSessionWithVenue(Course course, int durationMinutes, VenueGps venueGps) {
        String sessionId = mDatabase.child("Sessions").push().getKey();
        long startTime = System.currentTimeMillis();
        long expiryTime = startTime + ((long) durationMinutes * 60 * 1000);

        // Create session with embedded coordinates
        Session session = new Session(
                sessionId, 
                course.courseId, 
                course.courseCode, 
                course.courseName, 
                course.lecturerEmail, 
                venueGps.venueCode, // Use code from venue object
                venueGps.latitude, 
                venueGps.longitude, 
                venueGps.radius, 
                startTime, 
                expiryTime
        );

        if (sessionId != null) {
            mDatabase.child("Sessions").child(sessionId).setValue(session)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Session started at " + venueGps.venueName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to start session", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadRecentRedFlags() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase.child("RedFlags").orderByChild("reporterId").equalTo(uid).limitToLast(5)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        redFlagList.clear();
                        for (DataSnapshot flagSnap : snapshot.getChildren()) {
                            RedFlag flag = flagSnap.getValue(RedFlag.class);
                            if (flag != null) {
                                redFlagList.add(0, flag);
                            }
                        }
                        redFlagAdapter.notifyDataSetChanged();
                        noRedFlagsTextView.setVisibility(redFlagList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Lecturer lecturer = snapshot.getValue(Lecturer.class);
                        if (lecturer != null) {
                            if (lecturerNameTextView != null) {
                                lecturerNameTextView.setText(getString(R.string.welcome_lecturer, lecturer.getUsername()));
                            }
                            if (lecturerDeptTextView != null && lecturer.department != null) {
                                lecturerDeptTextView.setText(lecturer.department);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
            
            upcomingCountTextView.setText("0");
            pendingCountTextView.setText("0");
        }
    }

    private void loadActiveSessionsCount() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();
        if (email == null) return;

        mDatabase.child("Sessions").orderByChild("lecturerEmail").equalTo(email)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int activeCount = 0;
                        long currentTime = System.currentTimeMillis();
                        for (DataSnapshot sessionSnap : snapshot.getChildren()) {
                            Session session = sessionSnap.getValue(Session.class);
                            if (session != null) {
                                if (session.active) {
                                    if (session.expiryTime <= currentTime) {
                                        // Auto-Cleanup: Set active to false if expired
                                        sessionSnap.getRef().child("active").setValue(false);
                                    } else {
                                        activeCount++;
                                    }
                                }
                            }
                        }
                        if (pendingCountTextView != null) {
                            pendingCountTextView.setText(String.valueOf(activeCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void handleLogout() {
        mAuth.signOut();
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class RedFlagAdapter extends RecyclerView.Adapter<RedFlagAdapter.ViewHolder> {
        private List<RedFlag> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

        RedFlagAdapter(List<RedFlag> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_red_flag_small, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RedFlag flag = list.get(position);
            holder.summary.setText(flag.studentReg + " - " + flag.type);
            holder.time.setText(sdf.format(new Date(flag.timestamp)));
            
            int color = 0xFFFF4444; 
            if ("Low".equalsIgnoreCase(flag.severity)) color = 0xFFFFA500;
            else if ("Medium".equalsIgnoreCase(flag.severity)) color = 0xFFFF8C00;
            
            holder.indicator.setBackgroundColor(color);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView summary, time;
            View indicator;

            ViewHolder(View itemView) {
                super(itemView);
                summary = itemView.findViewById(R.id.flagSummary);
                time = itemView.findViewById(R.id.flagTime);
                indicator = findViewById(R.id.severityIndicator);
            }
        }
    }
}