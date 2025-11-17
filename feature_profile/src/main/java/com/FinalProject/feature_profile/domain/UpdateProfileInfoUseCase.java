package com.FinalProject.feature_profile.domain;

import com.FinalProject.feature_profile.data.ProfileRepository;
import com.FinalProject.feature_profile.model.ProfileInfo;

public class UpdateProfileInfoUseCase {

    public interface Callback {
        void onSuccess(ProfileInfo info);
        void onError(String message);
    }

    private final ProfileRepository repository;

    public UpdateProfileInfoUseCase(ProfileRepository repository) {
        this.repository = repository;
    }

    public void execute(ProfileInfo info, Callback callback) {
        repository.updateProfile(info, new ProfileRepository.ProfileUpdateCallback() {
            @Override
            public void onSuccess(ProfileInfo updated) {
                callback.onSuccess(updated);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
