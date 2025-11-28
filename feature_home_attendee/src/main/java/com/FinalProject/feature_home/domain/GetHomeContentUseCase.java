package com.FinalProject.feature_home.domain;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.feature_home.data.HomeRepository;
import com.FinalProject.feature_home.model.HomeContent;
import com.FinalProject.feature_home.model.HomeEvent;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

public class GetHomeContentUseCase {

    public interface Callback {
        void onSuccess(HomeContent content);
        void onError(String message);
    }

    private final HomeRepository repository;

    public GetHomeContentUseCase(HomeRepository repository) {
        this.repository = repository;
    }

    public void execute(Callback callback) {
        repository.loadHomeContent(new HomeRepository.HomeDataCallback() {
            @Override
            public void onSuccess(HomeContent content) {
                callback.onSuccess(content);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }


}
