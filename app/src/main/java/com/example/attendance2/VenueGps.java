package com.example.attendance2;

public  class VenueGps {
    public String venueCode;
    public String venueName;
    public double latitude;
    public double longitude;
    public int radius;

    public VenueGps() {}

    public VenueGps(String venueCode, String venueName, double latitude, double longitude, int radius) {
        this.venueCode = venueCode;
        this.venueName = venueName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }
}

