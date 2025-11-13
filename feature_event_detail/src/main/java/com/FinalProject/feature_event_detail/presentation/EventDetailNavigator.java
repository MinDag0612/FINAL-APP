package com.FinalProject.feature_event_detail.presentation;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Tiện ích tạo Intent mở màn hình chi tiết từ các module khác
 * mà không cần phụ thuộc trực tiếp vào implementation của Activity.
 */
public final class EventDetailNavigator {

    private EventDetailNavigator() {}

    public static Intent createIntent(Context context,
                                      String eventId,
                                      @Nullable String fallbackTitle,
                                      @Nullable String fallbackLocation,
                                      @Nullable String fallbackSchedule,
                                      long fallbackPrice) {
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, fallbackTitle);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_LOCATION, fallbackLocation);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_SCHEDULE, fallbackSchedule);
        if (fallbackPrice > 0) {
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_PRICE, fallbackPrice);
        }
        return intent;
    }
}
