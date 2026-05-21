package com.example.attendance2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddGpsActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText latitudeEditText, longitudeEditText, venueCodeEditText, venueNameEditText, radiusEditText;
    private MaterialButton fetchCurrentLocationButton, saveGpsButton,viewVenues;
    private BottomNavigationView adminBottomNavigation;

    private double currentLat = 0.0;
    private double currentLon = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_gps);

        mDatabase = FirebaseDatabase.getInstance("https://attendance2-6b6ad-default-rtdb.firebaseio.com/").getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        venueCodeEditText = findViewById(R.id.venueCodeEditText);
        venueNameEditText = findViewById(R.id.venueEditText);
        radiusEditText = findViewById(R.id.radiusEditText);
        fetchCurrentLocationButton = findViewById(R.id.fetchCurrentLocationButton);
        saveGpsButton = findViewById(R.id.saveGpsButton);
        viewVenues = findViewById(R.id.viewVenuesButton);
        adminBottomNavigation = findViewById(R.id.bottomNavigation);

        // Setup Bottom Navigation
        if (adminBottomNavigation != null) {
            adminBottomNavigation.setSelectedItemId(R.id.nav_gps);
            adminBottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_management) {
                    startActivity(new Intent(this, AdminDashboard.class));
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

        fetchCurrentLocationButton.setOnClickListener(v -> getCurrentLocation());

        saveGpsButton.setOnClickListener(v -> saveGpsData());

        viewVenues.setOnClickListener(v -> startActivity(new Intent(this, VenueActivity.class)));
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
            return;
        }

        Toast.makeText(this, "Fetching precise location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLon = location.getLongitude();
                        latitudeEditText.setText(String.format(Locale.getDefault(), "%.2f", currentLat));
                        longitudeEditText.setText(String.format(Locale.getDefault(), "%.2f", currentLon));
                        Toast.makeText(this, "Current location captured", Toast.LENGTH_SHORT).show();
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                            if (lastLoc != null) {
                                currentLat = lastLoc.getLatitude();
                                currentLon = lastLoc.getLongitude();
                                latitudeEditText.setText(String.format(Locale.getDefault(), "%.2f", currentLat));
                                longitudeEditText.setText(String.format(Locale.getDefault(), "%.2f", currentLon));
                                Toast.makeText(this, "Location captured (from cache)", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Could not get location. Ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveGpsData() {
        String venueCode = venueCodeEditText.getText().toString().trim();
        String venueName = venueNameEditText.getText().toString().trim();
        String latStr = latitudeEditText.getText().toString().trim();
        String lonStr = longitudeEditText.getText().toString().trim();
        String radiusStr = radiusEditText.getText().toString().trim();

        if (venueCode.isEmpty() || venueName.isEmpty() || latStr.isEmpty() || lonStr.isEmpty() || radiusStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            int radius = Integer.parseInt(radiusStr);

            VenueGps venueGps = new VenueGps(venueCode, venueName, lat, lon, radius);

            mDatabase.child("Venues").child(venueCode).setValue(venueGps)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Venue GPS configuration saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save configuration: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values for coordinates and radius", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
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

