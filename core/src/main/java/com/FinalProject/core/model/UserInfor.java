package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.Map;

public class UserInfor {
    private String fullname;
    private String phone;
    private String email;
    private String role;

    // Constructor rỗng (cần cho Firestore)
    public UserInfor() {}

    // Constructor đầy đủ
    public UserInfor(String fullname, String phone, String email, String role) {
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
        this.role = role;
    }

    // Getter & Setter
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Chuyển sang Map để push lên Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fullname", fullname);
        map.put("phone", phone);
        map.put("email", email);
        map.put("role", role);
        return map;
    }
}