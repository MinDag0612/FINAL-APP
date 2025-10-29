package com.example.feature_login.domain.usecase;

import com.example.feature_login.domain.models.Account;
import com.example.feature_login.domain.repository.AccountRepository;

public class LoginUseCase {
    private final AccountRepository repo;

    public LoginUseCase(AccountRepository repo){
        this.repo = repo;
    }

    public boolean excute(String username, String password, String role){
        if (repo.login(username, password, role) != null){
            return true;
        }
        return false;
    }

    public Account getAccount(String username, String password, String role){
        return repo.login(username, password, role);
    }
}
