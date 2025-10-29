package com.example.feature_login.domain.usecase;

import com.example.feature_login.domain.repository.UserInforRepository;

public class SignupUseCase {
    private final UserInforRepository userRepository;

    public SignupUseCase(UserInforRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean execute(String username, String email, String phone, String password, String role) {
        if (userRepository.signup(username, email, phone, password, role) != null){
            return true;
        }
        return false;
    }
}
