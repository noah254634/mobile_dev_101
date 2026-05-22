package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CourseDiscoveryActivity extends AppCompatActivity {

    private RecyclerView coursesRecyclerView;
    private TextInputEditText searchEditText;
    private TextView noCoursesTextView;
    private CourseAdapter adapter;
    private List<Course> courseList = new ArrayList<>();
    private List<Course> filteredList = new ArrayList<>();
    
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_discovery);

        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        coursesRecyclerView = findViewById(R.id.coursesRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        noCoursesTextView = findViewById(R.id.noCoursesTextView);

        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(filteredList);
        coursesRecyclerView.setAdapter(adapter);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_courses);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                finish(); // Go back to dashboard
                return true;
            } else if (itemId == R.id.nav_courses) {
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

        loadAllCourses();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllCourses() {
        mDatabase.child("Courses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Course course = postSnapshot.getValue(Course.class);
                    if (course != null) {
                        courseList.add(course);
                    }
                }
                filter(searchEditText.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CourseDiscoveryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        filteredList.clear();
        for (Course course : courseList) {
            if (course.courseName.toLowerCase().contains(text.toLowerCase()) ||
                course.courseCode.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(course);
            }
        }
        adapter.notifyDataSetChanged();
        noCoursesTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void enrollInCourse(Course course) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_enrollment)
                .setMessage(getString(R.string.enroll_confirm_msg, course.courseName))
                .setPositiveButton(R.string.enroll_button, (dialog, which) -> {
                    // Update user's profile
                    mDatabase.child("Users").child(currentUserId).child("enrolledCourses")
                            .child(course.courseId).setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                // Also update course's enrolled students list
                                mDatabase.child("Courses").child(course.courseId).child("enrolledStudents")
                                        .child(currentUserId).setValue(true);

                                Toast.makeText(CourseDiscoveryActivity.this, 
                                        getString(R.string.enrollment_success, course.courseCode), 
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {
        private List<Course> list;

        CourseAdapter(List<Course> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_discovery, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Course course = list.get(position);
            holder.courseName.setText(course.courseName);
            holder.courseCode.setText(course.courseCode);
            holder.lecturerName.setText(course.lecturerName);
            holder.venue.setText(course.venue);
            
            holder.btnEnroll.setOnClickListener(v -> enrollInCourse(course));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView courseName, courseCode, lecturerName, venue;
            MaterialButton btnEnroll;

            ViewHolder(View itemView) {
                super(itemView);
                courseName = itemView.findViewById(R.id.courseName);
                courseCode = itemView.findViewById(R.id.courseCode);
                lecturerName = itemView.findViewById(R.id.lecturerName);
                venue = itemView.findViewById(R.id.venue);
                btnEnroll = itemView.findViewById(R.id.btnEnroll);
            }
        }
    }
}
