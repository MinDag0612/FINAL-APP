package com.FinalProject.feature_profile.model;

import androidx.annotation.NonNull;

public class ProfileInfo {

    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;

    public ProfileInfo(String fullName, String email, String phone, String role) {
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.phone = phone != null ? phone : "";
        this.role = role != null ? role : "";
    }

    @NonNull
    public String getFullName() {
        return fullName;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getPhone() {
        return phone;
    }

    @NonNull
    public String getRole() {
        return role;
    }
}
