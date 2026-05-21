package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserMonitoringActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private TextInputEditText searchUserEditText;
    private TextView noUsersTextView;
    private UserAdapter adapter;
    private List<Users> userList = new ArrayList<>();
    private List<Users> filteredList = new ArrayList<>();
    
    private DatabaseReference mDatabase;
    private boolean showOnlyFlagged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_monitoring);

        showOnlyFlagged = getIntent().getBooleanExtra("filter_flagged", false);

        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        searchUserEditText = findViewById(R.id.searchUserEditText);
        noUsersTextView = findViewById(R.id.noUsersTextView);
        
        if (showOnlyFlagged) {
            TextView title = findViewById(R.id.userMonitoringTitle);
            if (title != null) title.setText("Flagged Accounts");
        }

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(filteredList);
        usersRecyclerView.setAdapter(adapter);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_monitoring);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                finish();
                return true;
            } else if (itemId == R.id.nav_management) {
                // Since this is in AdminDashboard activity, we can just finish or start activity.
                // For now, let's just go back to home as management is a dialog there.
                finish();
                return true;
            } else if (itemId == R.id.nav_monitoring) {
                return true;
            } else if (itemId == R.id.nav_logout) {
                handleLogout();
                return true;
            }
            return false;
        });

        loadAllUsers();

        searchUserEditText.addTextChangedListener(new TextWatcher() {
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

    private void loadAllUsers() {
        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null) {
                        userList.add(user);
                    }
                }
                filter(searchUserEditText.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserMonitoringActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        filteredList.clear();
        for (Users user : userList) {
            boolean matchesSearch = user.username.toLowerCase().contains(text.toLowerCase()) ||
                    user.email.toLowerCase().contains(text.toLowerCase());
            
            if (showOnlyFlagged) {
                if (user.isFlagged && matchesSearch) {
                    filteredList.add(user);
                }
            } else if (matchesSearch) {
                filteredList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
        noUsersTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void unflagUser(Users user) {
        if (user.userId == null) return;
        
        mDatabase.child("Users").child(user.userId).child("isFlagged").setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.account_unflagged), Toast.LENGTH_SHORT).show();
                    // Optional: You could also delete red flags for this user if desired, 
                    // but keeping them for records is usually better.
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private List<Users> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        UserAdapter(List<Users> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_monitor, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Users user = list.get(position);
            holder.username.setText(user.username);
            holder.email.setText(user.email);
            
            String roleText = user.role != null ? user.role.toUpperCase() : "UNKNOWN";
            if (user.isFlagged) {
                roleText += " (FLAGGED)";
                holder.roleTag.setTextColor(android.graphics.Color.RED);
                holder.btnUnflag.setVisibility(View.VISIBLE);
                holder.btnUnflag.setOnClickListener(v -> unflagUser(user));
            } else {
                holder.roleTag.setTextColor(androidx.core.content.ContextCompat.getColor(UserMonitoringActivity.this, R.color.primary_light));
                holder.btnUnflag.setVisibility(View.GONE);
            }
            holder.roleTag.setText(roleText);
            
            if (user.lastLogin > 0) {
                String dateStr = sdf.format(new Date(user.lastLogin));
                holder.lastLogin.setText(getString(R.string.last_login_label, dateStr));
            } else {
                holder.lastLogin.setText("Last Login: Never");
            }

            // Set icon based on role
            int iconRes = android.R.drawable.ic_menu_edit; // Default
            if (user.role != null) {
                if (user.role.contains("student")) iconRes = android.R.drawable.ic_menu_myplaces;
                else if (user.role.contains("lecturer")) iconRes = android.R.drawable.ic_menu_agenda;
                else if (user.role.contains("admin")) iconRes = android.R.drawable.ic_lock_power_off;
            }
            holder.roleIcon.setImageResource(iconRes);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView username, email, roleTag, lastLogin;
            ImageView roleIcon;
            MaterialButton btnUnflag;

            ViewHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.monitorUsername);
                email = itemView.findViewById(R.id.monitorEmail);
                roleTag = itemView.findViewById(R.id.monitorRoleTag);
                lastLogin = itemView.findViewById(R.id.monitorLastLogin);
                roleIcon = itemView.findViewById(R.id.userRoleIcon);
                btnUnflag = itemView.findViewById(R.id.btnUnflag);
            }
        }
    }
}
