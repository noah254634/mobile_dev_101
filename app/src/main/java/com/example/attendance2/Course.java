package com.example.attendance2;

public class Course {
    public String courseId;
    public String courseName;
    public String courseCode;
    public String lecturerName;
    public String lecturerEmail;
    public String venue;

    public Course() {
    }

    public Course(String courseId, String courseName, String courseCode, String lecturerName, String lecturerEmail, String venue) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.lecturerName = lecturerName;
        this.lecturerEmail = lecturerEmail;
        this.venue = venue;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public String getVenue() {
        return venue;
    }
}
