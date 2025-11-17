package com.FinalProject.feature_profile.domain;

import com.FinalProject.feature_profile.data.ProfileRepository;
import com.FinalProject.feature_profile.model.ProfileInfo;

public class GetProfileInfoUseCase {

    public interface Callback {
        void onSuccess(ProfileInfo info);
        void onError(String message);
    }

    private final ProfileRepository repository;

    public GetProfileInfoUseCase(ProfileRepository repository) {
        this.repository = repository;
    }

    public void execute(Callback callback) {
        repository.getProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProfileInfo info) {
                callback.onSuccess(info);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
