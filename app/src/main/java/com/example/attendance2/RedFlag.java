package com.example.attendance2;

public class RedFlag {
    public String flagId;
    public String studentReg;
    public String courseCode;
    public String description;
    public String type;
    public String severity;
    public long timestamp;
    public String reporterId;

    public RedFlag() {
    }

    public RedFlag(String flagId, String studentReg, String courseCode, String description, String type, String severity, long timestamp, String reporterId) {
        this.flagId = flagId;
        this.studentReg = studentReg;
        this.courseCode = courseCode;
        this.description = description;
        this.type = type;
        this.severity = severity;
        this.timestamp = timestamp;
        this.reporterId = reporterId;
    }
}
