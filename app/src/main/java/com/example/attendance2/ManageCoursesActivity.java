package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageCoursesActivity extends AppCompatActivity {

    private EditText editCourseName, editCourseCode;
    private Spinner lecturerSpinner, venueSpinner;
    private MaterialButton btnCreateCourse;
    private LinearLayout coursesListContainer;
    private TextView noCoursesTextView;
    private BottomNavigationView bottomNavigation;

    private DatabaseReference mDatabase;
    private List<Course> courseList = new ArrayList<>();
    private List<String> lecturerEmails = new ArrayList<>();
    private List<String> lecturerNames = new ArrayList<>();
    private List<String> venueCodes = new ArrayList<>();
    private List<String> venueNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_courses);

        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        editCourseName = findViewById(R.id.editCourseName);
        editCourseCode = findViewById(R.id.editCourseCode);
        lecturerSpinner = findViewById(R.id.lecturerSpinner);
        venueSpinner = findViewById(R.id.venueSpinner);
        btnCreateCourse = findViewById(R.id.btnCreateCourse);
        coursesListContainer = findViewById(R.id.coursesListContainer);
        noCoursesTextView = findViewById(R.id.noCoursesTextView);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Setup RecyclerView (actually LinearLayout container now)
        // coursesRecyclerView = findViewById(R.id.coursesRecyclerView); // Removed in previous overhaul

        // Load Data
        loadLecturers();
        loadVenues();
        loadCourses();

        // Setup Buttons
        btnCreateCourse.setOnClickListener(v -> createNewCourse());

        // Setup Navigation
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_management);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_management) {
                    return true;
                } else if (itemId == R.id.nav_monitoring) {
                    startActivity(new Intent(this, UserMonitoringActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    handleLogout();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadLecturers() {
        mDatabase.child("Users").orderByChild("role").equalTo("lecturer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lecturerEmails.clear();
                lecturerNames.clear();
                for (DataSnapshot lecturerSnap : snapshot.getChildren()) {
                    Lecturer lecturer = lecturerSnap.getValue(Lecturer.class);
                    if (lecturer != null) {
                        lecturerEmails.add(lecturer.email);
                        lecturerNames.add(lecturer.username + " (" + lecturer.email + ")");
                    }
                }
                ArrayAdapter<String> lecturerAdapter = new ArrayAdapter<>(ManageCoursesActivity.this, android.R.layout.simple_spinner_item, lecturerNames);
                lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lecturerSpinner.setAdapter(lecturerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageCoursesActivity.this, "Failed to load lecturers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVenues() {
        mDatabase.child("Venues").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                venueCodes.clear();
                venueNames.clear();
                for (DataSnapshot venueSnap : snapshot.getChildren()) {
                    VenueGps venue = venueSnap.getValue(VenueGps.class);
                    if (venue != null) {
                        venueCodes.add(venue.venueCode);
                        venueNames.add(venue.venueName + " (" + venue.venueCode + ")");
                    }
                }
                ArrayAdapter<String> venueAdapter = new ArrayAdapter<>(ManageCoursesActivity.this, android.R.layout.simple_spinner_item, venueNames);
                venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                venueSpinner.setAdapter(venueAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageCoursesActivity.this, "Failed to load venues", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCourses() {
        mDatabase.child("Courses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                coursesListContainer.removeAllViews();
                
                if (!snapshot.exists()) {
                    noCoursesTextView.setVisibility(View.VISIBLE);
                    return;
                }
                
                noCoursesTextView.setVisibility(View.GONE);
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Course course = postSnapshot.getValue(Course.class);
                    if (course != null) {
                        courseList.add(course);
                        addCourseView(course);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageCoursesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCourseView(Course course) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_course_discovery, coursesListContainer, false);
        
        TextView name = view.findViewById(R.id.courseName);
        TextView code = view.findViewById(R.id.courseCode);
        TextView lecturer = view.findViewById(R.id.lecturerName);
        TextView venue = view.findViewById(R.id.venue);
        MaterialButton btnEnroll = view.findViewById(R.id.btnEnroll);

        name.setText(course.courseName);
        code.setText(course.courseCode);
        lecturer.setText(course.lecturerName);
        venue.setText(course.venue);
        
        btnEnroll.setVisibility(View.GONE); // No enrollment from admin manage page

        coursesListContainer.addView(view);
    }

    private void createNewCourse() {
        String name = editCourseName.getText().toString().trim();
        String code = editCourseCode.getText().toString().trim();
        int lecturerPos = lecturerSpinner.getSelectedItemPosition();
        int venuePos = venueSpinner.getSelectedItemPosition();

        if (name.isEmpty() || code.isEmpty() || lecturerPos == -1 || venuePos == -1) {
            Toast.makeText(this, "Please fill all fields and select lecturer/venue", Toast.LENGTH_SHORT).show();
            return;
        }

        String lecturerEmail = lecturerEmails.get(lecturerPos);
        String lecturerName = lecturerNames.get(lecturerPos).split(" \\(")[0];
        String venueCode = venueCodes.get(venuePos);

        String courseId = mDatabase.child("Courses").push().getKey();
        Course newCourse = new Course(courseId, name, code, lecturerName, lecturerEmail, venueCode);

        if (courseId != null) {
            mDatabase.child("Courses").child(courseId).setValue(newCourse)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Course created successfully", Toast.LENGTH_SHORT).show();
                        editCourseName.setText("");
                        editCourseCode.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to create course", Toast.LENGTH_SHORT).show());
        }
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}