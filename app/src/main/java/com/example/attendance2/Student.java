package com.example.attendance2;

import java.util.HashMap;
import java.util.Map;

public class Student extends Users {
    public String deviceId;
    public String deviceName;
    public String registrationNumber;
    public String department;
    public Map<String, Boolean> enrolledCourses;

    public Student() {
        super();
        this.enrolledCourses = new HashMap<>();
    }

    public Student(String userId, String username, String email, String registrationNumber, String department, String deviceId, String deviceName) {
        super(userId, username, email, "student");
        this.registrationNumber = registrationNumber;
        this.department = department;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.enrolledCourses = new HashMap<>();
    }

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public Map<String, Boolean> getEnrolledCourses() { return enrolledCourses; }
    public void setEnrolledCourses(Map<String, Boolean> enrolledCourses) { this.enrolledCourses = enrolledCourses; }
}
