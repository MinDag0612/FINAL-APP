package com.FinalProject.feature_home.model;

public class HomeUser {
    private final String fullName;
    private final String email;
    private final String role;

    public HomeUser(String fullName, String email, String role) {
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.role = role != null ? role : "";
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getFirstName() {
        if (fullName.isEmpty()) {
            return "";
        }
        String[] parts = fullName.split(" ");
        return parts[parts.length - 1];
    }
}
