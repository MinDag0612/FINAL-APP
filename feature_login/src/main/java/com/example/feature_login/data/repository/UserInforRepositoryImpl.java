package com.example.feature_login.data.repository;

import com.example.feature_login.data.remote.FakeUserApi;
import com.example.feature_login.domain.models.UserInfor;
import com.example.feature_login.domain.repository.UserInforRepository;

public class UserInforRepositoryImpl implements UserInforRepository {
    private FakeUserApi api;

    public UserInforRepositoryImpl(FakeUserApi api) {
        this.api = api;
    }
    @Override
    public UserInfor signup(String username, String email, String phone, String password, String role) {
        return this.api.signup(username, email, phone, password, role);
    }
}
