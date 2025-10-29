package com.example.feature_login.domain.repository;

import com.example.feature_login.domain.models.Account;

public interface AccountRepository {
    Account login(String username, String password, String role);
}
