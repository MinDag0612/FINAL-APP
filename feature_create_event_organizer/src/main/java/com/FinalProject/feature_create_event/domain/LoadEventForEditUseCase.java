package com.FinalProject.feature_create_event.domain;

import com.FinalProject.feature_create_event.data.EventEditorRepository;

public class LoadEventForEditUseCase {
    private final EventEditorRepository repo = new EventEditorRepository();

    public interface Callback {
        void onSuccess(EventEditorRepository.EventWithTicket data);
        void onFailure(String message);
    }

    public void execute(String eventId, Callback callback) {
        if (eventId == null || eventId.isEmpty()) {
            if (callback != null) callback.onFailure("Thiếu eventId");
            return;
        }
        repo.loadEvent(eventId, new EventEditorRepository.LoadCallback() {
            @Override
            public void onSuccess(EventEditorRepository.EventWithTicket data) {
                if (callback != null) callback.onSuccess(data);
            }

            @Override
            public void onFailure(String message) {
                if (callback != null) callback.onFailure(message);
            }
        });
    }
}
