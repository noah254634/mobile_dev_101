package com.example.attendance2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VenueActivity extends AppCompatActivity {

    private RecyclerView venuesRecyclerView;
    private VenueAdapter venueAdapter;
    private List<VenueGps> venueList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private TextView venueCountTextView, noVenuesTextView;
    private FloatingActionButton addVenueFab;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_venue);
        
        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        venuesRecyclerView = findViewById(R.id.venuesRecyclerView);
        venueCountTextView = findViewById(R.id.venueCountTextView);
        noVenuesTextView = findViewById(R.id.noVenuesTextView);
        addVenueFab = findViewById(R.id.addVenueFab);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        venuesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        venueAdapter = new VenueAdapter(venueList);
        venuesRecyclerView.setAdapter(venueAdapter);

        addVenueFab.setOnClickListener(v -> {
            startActivity(new Intent(VenueActivity.this, AddGpsActivity.class));
        });

        if (bottomNavigation != null) {
            // Set correct menu based on user role would be ideal, but for now assuming admin access for this screen
            bottomNavigation.getMenu().clear();
            bottomNavigation.inflateMenu(R.menu.menu_admin_nav);
            // This screen doesn't have a direct nav item, but it's part of management/GPS
            bottomNavigation.setSelectedItemId(R.id.nav_gps); 
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_management) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_monitoring) {
                    startActivity(new Intent(this, UserMonitoringActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_gps) {
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    handleLogout();
                    return true;
                }
                return false;
            });
        }

        loadVenues();
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadVenues() {
        mDatabase.child("Venues").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                venueList.clear();
                for (DataSnapshot venueSnapshot : snapshot.getChildren()) {
                    VenueGps venue = venueSnapshot.getValue(VenueGps.class);
                    if (venue != null) {
                        venueList.add(venue);
                    }
                }
                venueAdapter.notifyDataSetChanged();
                
                int count = venueList.size();
                venueCountTextView.setText(count + " active geofence clusters");
                noVenuesTextView.setVisibility(venueList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VenueActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.ViewHolder> {
        private List<VenueGps> list;

        VenueAdapter(List<VenueGps> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_venue_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VenueGps venue = list.get(position);
            holder.venueName.setText(venue.venueName);
            holder.venueCode.setText(venue.venueCode);
            holder.latitude.setText(String.valueOf(venue.latitude));
            holder.longitude.setText(String.valueOf(venue.longitude));
            holder.radius.setText(venue.radius + "m Radius");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView venueName, venueCode, latitude, longitude, radius;

            ViewHolder(View itemView) {
                super(itemView);
                venueName = itemView.findViewById(R.id.itemVenueNameTextView);
                venueCode = itemView.findViewById(R.id.itemVenueCodeTextView);
                latitude = itemView.findViewById(R.id.itemLatitudeTextView);
                longitude = itemView.findViewById(R.id.itemLongitudeTextView);
                radius = itemView.findViewById(R.id.itemRadiusTextView);
            }
        }
    }
}