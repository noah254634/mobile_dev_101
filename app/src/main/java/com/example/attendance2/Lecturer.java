package com.example.attendance2;

import java.util.HashMap;
import java.util.Map;

public class Lecturer extends Users {
    public String department;
    public Map<String, Boolean> assignedCourses;

    public Lecturer() {
        super();
        this.assignedCourses = new HashMap<>();
    }

    public Lecturer(String userId, String username, String email, String department) {
        super(userId, username, email, "lecturer");
        this.department = department;
        this.assignedCourses = new HashMap<>();
    }

    public Map<String, Boolean> getAssignedCourses() { return assignedCourses; }
    public void setAssignedCourses(Map<String, Boolean> assignedCourses) { this.assignedCourses = assignedCourses; }
}
