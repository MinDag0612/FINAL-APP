package com.FinalProject.feature_review_event.presentation;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

public final class ReviewEventNavigator {

    private ReviewEventNavigator() {}

    public static Intent createIntent(Context context,
                                      String eventId,
                                      @Nullable String fallbackTitle,
                                      @Nullable String fallbackSchedule,
                                      @Nullable String fallbackLocation) {
        Intent intent = new Intent(context, ReviewEventActivity.class);
        intent.putExtra(ReviewEventActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(ReviewEventActivity.EXTRA_EVENT_TITLE, fallbackTitle);
        intent.putExtra(ReviewEventActivity.EXTRA_EVENT_SCHEDULE, fallbackSchedule);
        intent.putExtra(ReviewEventActivity.EXTRA_EVENT_LOCATION, fallbackLocation);
        return intent;
    }
}
