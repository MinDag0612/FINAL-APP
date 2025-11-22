package com.FinalProject.feature_home_organizer.domain;

import com.FinalProject.feature_home_organizer.data.OrganizerEventRepository;

import java.util.List;

public class GetOrganizerEventsUseCase {
    private final OrganizerEventRepository repo = new OrganizerEventRepository();

    public interface Callback {
        void onSuccess(List<OrganizerEventRepository.EventItem> events);
        void onFailure(String message);
    }

    public void execute(String organizerUid, Callback callback) {
        if (organizerUid == null || organizerUid.isEmpty()) {
            if (callback != null) callback.onFailure("Thiếu UID organizer");
            return;
        }
        repo.loadEvents(organizerUid, new OrganizerEventRepository.Callback() {
            @Override
            public void onSuccess(List<OrganizerEventRepository.EventItem> events) {
                if (callback != null) callback.onSuccess(events);
            }

            @Override
            public void onFailure(String message) {
                if (callback != null) callback.onFailure(message);
            }
        });
    }
}
