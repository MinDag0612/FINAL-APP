package com.FinalProject.feature_event_detail.domain;

import com.FinalProject.feature_event_detail.data.EventDetailRepository;
import com.FinalProject.feature_event_detail.model.EventDetail;

public class GetEventDetailUseCase {

    public interface Callback {
        void onSuccess(EventDetail detail);
        void onError(String message);
    }

    private final EventDetailRepository repository;

    public GetEventDetailUseCase(EventDetailRepository repository) {
        this.repository = repository;
    }

    public void execute(String eventId, Callback callback) {
        repository.getEventDetail(eventId, new EventDetailRepository.Callback() {
            @Override
            public void onSuccess(EventDetail detail) {
                callback.onSuccess(detail);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
