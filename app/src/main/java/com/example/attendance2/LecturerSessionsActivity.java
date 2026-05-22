package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LecturerSessionsActivity extends AppCompatActivity {

    private RecyclerView sessionsRecyclerView;
    private TextView noSessionsTextView;
    private SessionAdapter adapter;
    private List<Session> sessionList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_sessions);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        noSessionsTextView = findViewById(R.id.noSessionsTextView);

        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(sessionList);
        sessionsRecyclerView.setAdapter(adapter);

        setupNavigation();
        loadSessions();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_sessions);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_sessions) {
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    Toast.makeText(this, "Reports coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    mAuth.signOut();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadSessions() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();
        if (email == null) return;

        mDatabase.child("Sessions").orderByChild("lecturerEmail").equalTo(email)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sessionList.clear();
                        long currentTime = System.currentTimeMillis();
                        for (DataSnapshot sessionSnap : snapshot.getChildren()) {
                            Session session = sessionSnap.getValue(Session.class);
                            if (session != null) {
                                // Auto-Cleanup: Set active to false if expired
                                if (session.active && session.expiryTime <= currentTime) {
                                    sessionSnap.getRef().child("active").setValue(false);
                                    session.active = false;
                                }
                                sessionList.add(session);
                            }
                        }
                        Collections.reverse(sessionList);
                        adapter.notifyDataSetChanged();
                        noSessionsTextView.setVisibility(sessionList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LecturerSessionsActivity.this, "Failed to load sessions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
        private List<Session> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

        SessionAdapter(List<Session> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_lecturer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Session session = list.get(position);
            holder.courseName.setText(session.courseName + " (" + session.courseCode + ")");
            holder.time.setText("Started: " + sdf.format(new Date(session.startTime)));
            holder.venue.setText(session.venue);

            long currentTime = System.currentTimeMillis();
            if (session.active && session.expiryTime > currentTime) {
                holder.status.setText("ACTIVE");
                holder.status.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                holder.status.setText("EXPIRED");
                holder.status.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }

            // Load attendance count
            mDatabase.child("Attendance").orderByChild("sessionId").equalTo(session.sessionId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            long count = snapshot.getChildrenCount();
                            holder.count.setText(count + " Students");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(LecturerSessionsActivity.this, SessionDetailsActivity.class);
                intent.putExtra("sessionId", session.sessionId);
                intent.putExtra("courseName", session.courseName);
                intent.putExtra("courseCode", session.courseCode);
                intent.putExtra("startTime", session.startTime);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView courseName, time, status, venue, count;

            ViewHolder(View itemView) {
                super(itemView);
                courseName = itemView.findViewById(R.id.sessionCourseName);
                time = itemView.findViewById(R.id.sessionTime);
                status = itemView.findViewById(R.id.sessionStatus);
                venue = itemView.findViewById(R.id.sessionVenue);
                count = itemView.findViewById(R.id.attendanceCount);
            }
        }
    }
}
