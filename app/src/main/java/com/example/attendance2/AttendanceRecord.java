package com.example.attendance2;

public class AttendanceRecord {
    public String attendanceId;
    public String sessionId;
    public String studentId;
    public String studentReg;
    public String courseId;
    public long timestamp;
    public String deviceId;
    public double latitude;
    public double longitude;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String attendanceId, String sessionId, String studentId, String studentReg, String courseId, long timestamp, String deviceId, double latitude, double longitude) {
        this.attendanceId = attendanceId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentReg = studentReg;
        this.courseId = courseId;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
