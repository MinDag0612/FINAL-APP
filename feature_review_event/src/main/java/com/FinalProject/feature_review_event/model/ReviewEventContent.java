package com.FinalProject.feature_review_event.model;

import androidx.annotation.NonNull;

/**
 * Dữ liệu hiển thị ở màn hình viết review sau khi tải từ Firestore.
 */
public class ReviewEventContent {

    private final String eventName;
    private final String eventSchedule;
    private final String eventLocation;
    private final boolean alreadyReviewed;

    public ReviewEventContent(@NonNull String eventName,
                              @NonNull String eventSchedule,
                              @NonNull String eventLocation,
                              boolean alreadyReviewed) {
        this.eventName = eventName;
        this.eventSchedule = eventSchedule;
        this.eventLocation = eventLocation;
        this.alreadyReviewed = alreadyReviewed;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventSchedule() {
        return eventSchedule;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public boolean isAlreadyReviewed() {
        return alreadyReviewed;
    }
}
