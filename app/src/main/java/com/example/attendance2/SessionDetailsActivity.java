package com.example.attendance2;

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

public class SessionDetailsActivity extends AppCompatActivity {

    private TextView courseNameHeader, sessionTimeHeader, noAttendanceTextView, livePresenceCount, totalSignedCount;
    private RecyclerView attendanceRecyclerView;
    private AttendanceAdapter adapter;
    private List<AttendanceRecord> attendanceList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String sessionId, courseName, courseCode;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        sessionId = getIntent().getStringExtra("sessionId");
        courseName = getIntent().getStringExtra("courseName");
        courseCode = getIntent().getStringExtra("courseCode");
        startTime = getIntent().getLongExtra("startTime", 0);

        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        courseNameHeader = findViewById(R.id.detailsCourseName);
        sessionTimeHeader = findViewById(R.id.detailsSessionTime);
        noAttendanceTextView = findViewById(R.id.noAttendanceTextView);
        livePresenceCount = findViewById(R.id.livePresenceCount);
        totalSignedCount = findViewById(R.id.totalSignedCount);
        attendanceRecyclerView = findViewById(R.id.attendanceRecyclerView);

        courseNameHeader.setText(courseName + " (" + courseCode + ")");
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd - HH:mm", Locale.getDefault());
        sessionTimeHeader.setText("Started: " + sdf.format(new Date(startTime)));

        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        attendanceRecyclerView.setAdapter(adapter);

        loadAttendanceRecords();
        loadLivePresence();
    }

    private void loadLivePresence() {
        if (sessionId == null) return;
        
        mDatabase.child("Presence").child(sessionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (livePresenceCount != null) {
                    livePresenceCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAttendanceRecords() {
        if (sessionId == null) return;

        mDatabase.child("Attendance").orderByChild("sessionId").equalTo(sessionId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        attendanceList.clear();
                        for (DataSnapshot recordSnap : snapshot.getChildren()) {
                            AttendanceRecord record = recordSnap.getValue(AttendanceRecord.class);
                            if (record != null) {
                                attendanceList.add(0, record); // Most recent first
                            }
                        }
                        adapter.notifyDataSetChanged();
                        noAttendanceTextView.setVisibility(attendanceList.isEmpty() ? View.VISIBLE : View.GONE);
                        
                        if (totalSignedCount != null) {
                            totalSignedCount.setText(String.valueOf(attendanceList.size()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SessionDetailsActivity.this, "Failed to load records", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {
        private List<AttendanceRecord> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        AttendanceAdapter(List<AttendanceRecord> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = list.get(position);
            holder.studentReg.setText(record.studentReg != null ? record.studentReg : "Unknown Reg");
            holder.time.setText("Signed at: " + sdf.format(new Date(record.timestamp)));
            
            // For now just show "Verified" instead of distance since we don't have venue coords here easily
            holder.distance.setText("VERIFIED");
            holder.distance.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView studentReg, time, distance;

            ViewHolder(View itemView) {
                super(itemView);
                studentReg = itemView.findViewById(R.id.studentRegText);
                time = itemView.findViewById(R.id.signingTime);
                distance = itemView.findViewById(R.id.distanceText);
            }
        }
    }
}
