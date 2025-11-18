package com.FinalProject.feature_review_event.domain;

import com.FinalProject.feature_review_event.data.ReviewEventRepository;
import com.FinalProject.feature_review_event.model.ReviewEventContent;

public class LoadReviewEventInfoUseCase {

    public interface Callback {
        void onSuccess(ReviewEventContent content);
        void onError(String message);
    }

    private final ReviewEventRepository repository;

    public LoadReviewEventInfoUseCase(ReviewEventRepository repository) {
        this.repository = repository;
    }

    public void execute(String eventId, Callback callback) {
        repository.loadReviewContent(eventId, new ReviewEventRepository.ReviewContentCallback() {
            @Override
            public void onSuccess(ReviewEventContent content) {
                callback.onSuccess(content);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
