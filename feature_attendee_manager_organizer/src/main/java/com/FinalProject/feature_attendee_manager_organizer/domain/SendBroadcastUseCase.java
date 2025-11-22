package com.FinalProject.feature_attendee_manager_organizer.domain;

import android.text.TextUtils;

import com.FinalProject.feature_attendee_manager_organizer.data.AttendeeRepository;
import com.FinalProject.feature_attendee_manager_organizer.data.NotificationRepository;

import java.util.List;

public class SendBroadcastUseCase {
    private final NotificationRepository repo = new NotificationRepository();

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public void execute(String eventId, String eventName, String title, String content, List<AttendeeRepository.AttendeeItem> attendees, Callback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onFailure("Thiếu eventId");
            return;
        }
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            callback.onFailure("Vui lòng nhập tiêu đề và nội dung");
            return;
        }
        repo.sendBroadcast(eventId, eventName, title, content, attendees)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
