package com.example.attendance2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudentsDashboardActivity extends AppCompatActivity {
    private static final String TAG = "StudentsDashboard";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView welcomeTextView, emailTextView, deviceTextView, timerTextView, deviceIdStatusTextView, currentLatitude, currentLongitude;
    private TextView regNumberTextView, deptTextView, courseStatCount;
    private LinearLayout coursesContainer;
    private MaterialButton logoutButton, enrollCourseButton, viewReportButton, markAttendanceButton;
    private String currentUserId;
    private String currentDeviceId;
    private Session currentActiveSession;
    private String studentRegistrationNumber;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private Set<String> enrolledCourseIds = new HashSet<>();
    private ValueEventListener sessionsListener;
    private boolean isDeviceCheckInProgress = false;
    private AlertDialog flaggedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_students_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        welcomeTextView = findViewById(R.id.welcomeTextView);
        emailTextView = findViewById(R.id.emailTextView);
        deviceTextView = findViewById(R.id.deviceTextView);
        timerTextView = findViewById(R.id.timerTextView);
        deviceIdStatusTextView = findViewById(R.id.deviceIdStatusTextView);
        regNumberTextView = findViewById(R.id.regNumberTextView);
        deptTextView = findViewById(R.id.deptTextView);
        courseStatCount = findViewById(R.id.courseStatCount);
        currentLatitude = findViewById(R.id.currentLatitude);
        currentLongitude = findViewById(R.id.currentLongitude);

        coursesContainer = findViewById(R.id.coursesContainer);
        logoutButton = findViewById(R.id.logoutButton);
        enrollCourseButton = findViewById(R.id.enrollCourseButton);
        viewReportButton = findViewById(R.id.viewReportButton);
        markAttendanceButton = findViewById(R.id.markAttendanceButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupNavigation();
        startAutoLocationUpdates();

        if (currentUserId == null) {
            redirectToLogin();
            return;
        }

        checkAccountSecurity(); // real-time and device binding
        loadStudentData();
        loadEnrolledCourses();

        logoutButton.setOnClickListener(v -> handleLogout());
        enrollCourseButton.setOnClickListener(v -> {
            startActivity(new Intent(this, CourseDiscoveryActivity.class));
        });
        viewReportButton.setOnClickListener(v -> {
            Toast.makeText(this, "Reports coming soon", Toast.LENGTH_SHORT).show();
        });
        markAttendanceButton.setOnClickListener(v -> handleMarkAttendance());
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_courses) {
                    startActivity(new Intent(this, CourseDiscoveryActivity.class));
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

    private void startAutoLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Fetch location immediately on start
        updateLocationDisplay();

        // Also setup a recurring check every 30 seconds for telemetry
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLocationDisplay();
                timerHandler.postDelayed(this, 30000);
            }
        }, 30000);
    }

    private void updateLocationDisplay() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude.setText(String.format(Locale.getDefault(), "%.2f", location.getLatitude()));
                        currentLongitude.setText(String.format(Locale.getDefault(), "%.2f", location.getLongitude()));
                    }
                });
    }

    private void handleMarkAttendance() {
        if (currentActiveSession == null) {
            Toast.makeText(this, R.string.no_active_session, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        Toast.makeText(this, "Verifying location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude.setText(String.format(Locale.getDefault(), "%.2f", location.getLatitude()));
                        currentLongitude.setText(String.format(Locale.getDefault(), "%.2f", location.getLongitude()));
                        verifyLocationAndMark(location);
                    } else {
                        Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyLocationAndMark(Location studentLocation) {
        mDatabase.child("Venues").child(currentActiveSession.venue).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                VenueGps venue = snapshot.getValue(VenueGps.class);
                if (venue != null) {
                    // Round student coordinates to 2 decimal places
                    double roundedStudentLat = Math.round(studentLocation.getLatitude() * 100.0) / 100.0;
                    double roundedStudentLon = Math.round(studentLocation.getLongitude() * 100.0) / 100.0;

                    // Round venue coordinates to 2 decimal places
                    double roundedVenueLat = Math.round(venue.latitude * 100.0) / 100.0;
                    double roundedVenueLon = Math.round(venue.longitude * 100.0) / 100.0;

                    if (roundedStudentLat == roundedVenueLat && roundedStudentLon == roundedVenueLon) {
                        saveAttendanceRecord(studentLocation);
                    } else {
                        // Logic fallback: check distance if rounding doesn't match perfectly
                        float[] results = new float[1];
                        Location.distanceBetween(studentLocation.getLatitude(), studentLocation.getLongitude(),
                                venue.latitude, venue.longitude, results);
                        float distanceInMeters = results[0];

                        if (distanceInMeters <= venue.radius) {
                            saveAttendanceRecord(studentLocation);
                        } else {
                            Toast.makeText(StudentsDashboardActivity.this, R.string.out_of_range, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(StudentsDashboardActivity.this, "Venue GPS not configured by Admin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveAttendanceRecord(Location location) {
        String attendanceId = mDatabase.child("Attendance").push().getKey();
        long timestamp = System.currentTimeMillis();

        AttendanceRecord record = new AttendanceRecord(
                attendanceId,
                currentActiveSession.sessionId,
                currentUserId,
                studentRegistrationNumber,
                currentActiveSession.courseId,
                timestamp,
                currentDeviceId,
                location.getLatitude(),
                location.getLongitude()
        );

        if (attendanceId != null) {
            mDatabase.child("Attendance").child(attendanceId).setValue(record)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.attendance_marked, Toast.LENGTH_SHORT).show();
                        markAttendanceButton.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save attendance", Toast.LENGTH_SHORT).show());
        }
    }

    private void checkAccountSecurity() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mDatabase.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Student student = snapshot.getValue(Student.class);
                if (student != null) {
                    if (student.isFlagged) {
                        showFlaggedAccountDialog();
                        return;
                    }

                    if (student.deviceId == null) {
                        checkDeviceBinding(androidId, deviceName, student);
                    } else if (!student.deviceId.equals(androidId)) {
                        flagAndMismatch(androidId, student);
                    } else {
                        currentDeviceId = androidId;
                        updateDeviceInfoUi(androidId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkDeviceBinding(String androidId, String deviceName, Student student) {
        if (isDeviceCheckInProgress) return;
        isDeviceCheckInProgress = true;

        checkForExistingDeviceOwner(androidId, deviceName, student);
    }

    private void showFlaggedAccountDialog() {
        if (flaggedDialog != null && flaggedDialog.isShowing()) return;
        
        flaggedDialog = new AlertDialog.Builder(this)
                .setTitle("Account Restricted")
                .setMessage("This account has been flagged for security violations (Device Sharing or unauthorized login). Please contact the administrator.")
                .setPositiveButton("Logout", (dialog, which) -> handleLogout())
                .setCancelable(false)
                .show();
    }

    private void checkForExistingDeviceOwner(String androidId, String deviceName, Student currentStudent) {
        mDatabase.child("Users").orderByChild("deviceId").equalTo(androidId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isDeviceCheckInProgress = false;
                boolean anotherOwnerFound = false;
                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String foundUserId = userSnap.getKey();
                        if (foundUserId != null && foundUserId.equals(currentUserId)) {
                            continue; 
                        }
                        
                        Student otherStudent = userSnap.getValue(Student.class);
                        if (otherStudent != null) {
                            reportIdentityAnomaly(currentStudent, otherStudent, androidId);
                            anotherOwnerFound = true;
                        }
                    }
                }
                
                if (anotherOwnerFound) {
                    showDeviceMismatchDialog("Another registered student's device");
                } else {
                    bindDevice(androidId, deviceName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isDeviceCheckInProgress = false;
            }
        });
    }

    private void flagAndMismatch(String androidId, Student student) {
        mDatabase.child("Users").orderByChild("deviceId").equalTo(androidId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean sharedDevice = false;
                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String foundUserId = userSnap.getKey();
                        if (foundUserId != null && foundUserId.equals(currentUserId)) {
                            continue; 
                        }
                        
                        Student otherStudent = userSnap.getValue(Student.class);
                        if (otherStudent != null) {
                            reportIdentityAnomaly(student, otherStudent, androidId);
                            sharedDevice = true;
                        }
                    }
                }
                
                if (!sharedDevice) {
                    reportDeviceMismatch(student, androidId);
                }
                showDeviceMismatchDialog(student.deviceName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void reportIdentityAnomaly(Student intruder, Student owner, String deviceId) {
        String flagId = mDatabase.child("RedFlags").push().getKey();
        if (flagId == null) return;

        Map<String, Object> flagData = new HashMap<>();
        flagData.put("flagId", flagId);
        flagData.put("studentReg", intruder.registrationNumber + " / " + owner.registrationNumber);
        flagData.put("courseCode", "SYSTEM_SECURITY");
        flagData.put("description", "Identity Anomaly: User " + intruder.username + " (" + intruder.registrationNumber + 
                ") attempted login on device owned by " + owner.username + " (" + owner.registrationNumber + "). Device ID: " + deviceId);
        flagData.put("type", "Device Sharing Detected");
        flagData.put("severity", "Critical");
        flagData.put("timestamp", System.currentTimeMillis());
        flagData.put("reporterId", "SYSTEM_AUTO");

        mDatabase.child("RedFlags").child(flagId).setValue(flagData);
        
        mDatabase.child("Users").child(intruder.userId).child("isFlagged").setValue(true);
        mDatabase.child("Users").child(owner.userId).child("isFlagged").setValue(true);
    }

    private void reportDeviceMismatch(Student student, String intruderDeviceId) {
        String flagId = mDatabase.child("RedFlags").push().getKey();
        if (flagId == null) return;

        Map<String, Object> flagData = new HashMap<>();
        flagData.put("flagId", flagId);
        flagData.put("studentReg", student.registrationNumber);
        flagData.put("courseCode", "SYSTEM_SECURITY");
        flagData.put("description", "Device Mismatch: User " + student.username + " attempted login on unauthorized device: " + intruderDeviceId);
        flagData.put("type", "Unauthorized Device Attempt");
        flagData.put("severity", "Medium");
        flagData.put("timestamp", System.currentTimeMillis());
        flagData.put("reporterId", "SYSTEM_AUTO");

        mDatabase.child("RedFlags").child(flagId).setValue(flagData);
        mDatabase.child("Users").child(student.userId).child("isFlagged").setValue(true);
    }

    private void updateDeviceInfoUi(String deviceId) {
        if (deviceTextView != null) {
            deviceTextView.setText(getString(R.string.authorized_device_secure));
            deviceTextView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, 0, 0);
        }
        if (deviceIdStatusTextView != null) {
            deviceIdStatusTextView.setText("ID: " + deviceId);
        }
    }

    private void bindDevice(String deviceId, String deviceName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("deviceId", deviceId);
        updates.put("deviceName", deviceName);

        mDatabase.child("Users").child(currentUserId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    currentDeviceId = deviceId;
                    Toast.makeText(this, R.string.device_bound_success, Toast.LENGTH_SHORT).show();
                    updateDeviceInfoUi(deviceId);
                });
    }

    private void showDeviceMismatchDialog(String originalDevice) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.device_mismatch)
                .setMessage("This account is securely linked to: " + originalDevice + ". This incident has been logged for administrative review.")
                .setPositiveButton("Logout", (dialog, which) -> handleLogout())
                .setCancelable(false)
                .show();
    }

    private void loadStudentData() {
        mDatabase.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Student student = snapshot.getValue(Student.class);
                if (student != null) {
                    if (student.isFlagged) {
                        showFlaggedAccountDialog();
                        return;
                    }

                    studentRegistrationNumber = student.registrationNumber;
                    welcomeTextView.setText("Welcome, " + student.username + "!");
                    emailTextView.setText(student.email);
                    if (student.registrationNumber != null) {
                        regNumberTextView.setText(student.registrationNumber);
                    }
                    if (student.department != null) {
                        deptTextView.setText(student.department);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadEnrolledCourses() {
        mDatabase.child("Users").child(currentUserId).child("enrolledCourses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                coursesContainer.removeAllViews();
                enrolledCourseIds.clear();
                long count = snapshot.getChildrenCount();
                if (courseStatCount != null) {
                    courseStatCount.setText(String.valueOf(count));
                }

                if (!snapshot.exists()) {
                    TextView noCourses = new TextView(StudentsDashboardActivity.this);
                    noCourses.setText("No courses enrolled yet.");
                    noCourses.setTextColor(getResources().getColor(R.color.text_grey));
                    noCourses.setPadding(0, 40, 0, 40);
                    coursesContainer.addView(noCourses);
                    updateActiveSessionsListener();
                    return;
                }

                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    String courseId = courseSnapshot.getKey();
                    if (courseId != null) {
                        enrolledCourseIds.add(courseId);
                        fetchAndDisplayCourse(courseId);
                    }
                }
                updateActiveSessionsListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateActiveSessionsListener() {
        if (sessionsListener != null) {
            mDatabase.child("Sessions").removeEventListener(sessionsListener);
        }

        sessionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                boolean found = false;
                for (DataSnapshot sessionSnap : snapshot.getChildren()) {
                    Session session = sessionSnap.getValue(Session.class);
                    if (session != null && session.active && session.expiryTime > currentTime && enrolledCourseIds.contains(session.courseId)) {
                        currentActiveSession = session;
                        checkIfAttendanceMarked(session);
                        displayActiveSession(session);
                        found = true;
                        break; 
                    }
                }
                if (!found) {
                    currentActiveSession = null;
                    stopTimer();
                    resetTimerUi();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Sessions").addValueEventListener(sessionsListener);
    }

    private void checkIfAttendanceMarked(Session session) {
        mDatabase.child("Attendance").orderByChild("sessionId").equalTo(session.sessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean marked = false;
                        for (DataSnapshot recordSnap : snapshot.getChildren()) {
                            AttendanceRecord record = recordSnap.getValue(AttendanceRecord.class);
                            if (record != null && record.studentId.equals(currentUserId)) {
                                marked = true;
                                break;
                            }
                        }
                        if (marked) {
                            markAttendanceButton.setVisibility(View.GONE);
                        } else {
                            markAttendanceButton.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void displayActiveSession(Session session) {
        if (timerTextView != null) {
            long remaining = session.expiryTime - System.currentTimeMillis();
            if (remaining > 0) {
                long minutes = remaining / (60 * 1000);
                long seconds = (remaining % (60 * 1000)) / 1000;
                timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d Remaining (%s)", minutes, seconds, session.courseCode));
                timerTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                currentActiveSession = null;
                stopTimer();
                resetTimerUi();
                return;
            }
        }

        if (timerRunnable == null) {
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (currentActiveSession != null) {
                        displayActiveSession(currentActiveSession);
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        stopTimer();
                    }
                }
            };
            timerHandler.post(timerRunnable);
        }
    }

    private void resetTimerUi() {
        if (timerTextView != null) {
            timerTextView.setText("No Active Session");
            timerTextView.setTextColor(getResources().getColor(R.color.primary_light));
        }
        if (markAttendanceButton != null) {
            markAttendanceButton.setVisibility(View.GONE);
        }
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (sessionsListener != null) {
            mDatabase.child("Sessions").removeEventListener(sessionsListener);
        }
    }

    private void fetchAndDisplayCourse(String courseId) {
        mDatabase.child("Courses").child(courseId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Course course = snapshot.getValue(Course.class);
                if (course != null) {
                    addCourseToUi(course);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCourseToUi(Course course) {
        View courseView = LayoutInflater.from(this).inflate(R.layout.item_course_student, coursesContainer, false);
        TextView name = courseView.findViewById(R.id.itemCourseName);
        TextView code = courseView.findViewById(R.id.itemCourseCode);
        TextView lecturer = courseView.findViewById(R.id.itemCourseLecturer);
        TextView venue = courseView.findViewById(R.id.itemCourseVenue);

        name.setText(course.courseName);
        code.setText(course.courseCode);
        lecturer.setText("Lecturer: " + course.lecturerName);
        venue.setText("Venue: " + course.venue);

        coursesContainer.addView(courseView);
    }

    private void handleLogout() {
        mAuth.signOut();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(StudentsDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleMarkAttendance();
            } else {
                Toast.makeText(this, "Location permission is required to mark attendance", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
