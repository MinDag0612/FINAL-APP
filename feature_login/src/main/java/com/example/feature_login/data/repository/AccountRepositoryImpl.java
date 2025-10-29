package com.example.feature_login.data.repository;

import com.example.feature_login.data.remote.FakeUserApi;
import com.example.feature_login.domain.models.Account;
import com.example.feature_login.domain.repository.AccountRepository;


public class AccountRepositoryImpl implements AccountRepository {
    private FakeUserApi api;

    public AccountRepositoryImpl(FakeUserApi api){
        this.api = api;
    }

    @Override
    public Account login(String username, String password, String role) {
        return this.api.login(username, password, role);
    }


}
