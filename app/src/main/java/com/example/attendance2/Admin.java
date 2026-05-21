package com.example.attendance2;

public class Admin extends Users {
    public Admin() {
        super();
    }

    public Admin(String userId, String username, String email) {
        super(userId, username, email, "admin");
    }
}
