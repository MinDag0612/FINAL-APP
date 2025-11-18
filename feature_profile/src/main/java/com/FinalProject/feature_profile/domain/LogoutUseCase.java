package com.FinalProject.feature_profile.domain;

import com.FinalProject.feature_profile.data.ProfileRepository;

public class LogoutUseCase {

    private final ProfileRepository repository;

    public LogoutUseCase(ProfileRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        repository.logout();
    }
}
