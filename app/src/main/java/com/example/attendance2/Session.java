package com.example.attendance2;

public class Session {
    public String sessionId;
    public String courseId;
    public String courseCode;
    public String courseName;
    public String lecturerEmail;
    public String venue;
    public double latitude;
    public double longitude;
    public int radius;
    public long startTime;
    public long expiryTime;
    public boolean active;

    public Session() {
    }

    public Session(String sessionId, String courseId, String courseCode, String courseName, String lecturerEmail, String venue, long startTime, long expiryTime) {
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturerEmail = lecturerEmail;
        this.venue = venue;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        this.active = true;
    }

    public Session(String sessionId, String courseId, String courseCode, String courseName, String lecturerEmail, String venue, double latitude, double longitude, int radius, long startTime, long expiryTime) {
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturerEmail = lecturerEmail;
        this.venue = venue;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        this.active = true;
    }
}
