package com.FinalProject.feature_attendee_manager_organizer.domain;

import com.FinalProject.feature_attendee_manager_organizer.data.AttendeeRepository;

import java.util.List;

public class GetAttendeesUseCase {
    private final AttendeeRepository repo = new AttendeeRepository();

    public interface Callback {
        void onSuccess(List<AttendeeRepository.AttendeeItem> attendees);
        void onFailure(String message);
    }

    public void execute(String eventId, Callback callback) {
        repo.loadAttendees(eventId, new AttendeeRepository.Callback() {
            @Override
            public void onSuccess(List<AttendeeRepository.AttendeeItem> attendees) {
                if (callback != null) callback.onSuccess(attendees);
            }

            @Override
            public void onFailure(String message) {
                if (callback != null) callback.onFailure(message);
            }
        });
    }
}
