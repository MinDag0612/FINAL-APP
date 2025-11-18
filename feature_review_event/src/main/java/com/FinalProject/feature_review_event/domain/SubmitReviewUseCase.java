package com.FinalProject.feature_review_event.domain;

import com.FinalProject.feature_review_event.data.ReviewEventRepository;

public class SubmitReviewUseCase {

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    private final ReviewEventRepository repository;

    public SubmitReviewUseCase(ReviewEventRepository repository) {
        this.repository = repository;
    }

    public void execute(String eventId, int rating, String comment, Callback callback) {
        repository.submitReview(eventId, rating, comment, new ReviewEventRepository.SubmitCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
