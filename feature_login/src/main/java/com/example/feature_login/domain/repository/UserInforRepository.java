package com.example.feature_login.domain.repository;

import com.example.feature_login.domain.models.UserInfor;

public interface UserInforRepository {
    UserInfor signup(String username, String email, String phone, String password, String role);
}
