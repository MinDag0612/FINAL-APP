package com.example.feature_login.domain.models;

public class UserInfor {
    private int id;
    private String fullname;
    private String email;
    private String phone;
    private String password;
    String role;

    public UserInfor(int id, String fullname, String email, String phone, String password, String role) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }
}
