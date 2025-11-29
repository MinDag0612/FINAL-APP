package com.FinalProject.feature_booking.presentation;

import android.content.Context;
import android.content.Intent;

/**
 * Utility to open booking flow (BookingHostActivity) from other modules without hard dependency on implementation.
 */
public final class BookingNavigator {

    private BookingNavigator() {}

    private static final String EXTRA_EVENT_ID = "eventId";
    private static final String EXTRA_SHOW_ID = "showId";
    private static final String EXTRA_EVENT_TITLE = "eventTitle";

    /**
     * @param eventId   required event identifier
     * @param eventTitle optional name/title, used for toolbar
     * @param showId     optional show/seating session id; will default to eventId + "_DEFAULT" if missing
     */
    public static Intent createBookingIntent(Context context, String eventId, String eventTitle, String showId) {
        Intent intent = new Intent(context, BookingHostActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_EVENT_TITLE, eventTitle);
        intent.putExtra(EXTRA_SHOW_ID, ensureShowId(eventId, showId));
        return intent;
    }

    private static String ensureShowId(String eventId, String showId) {
        if (showId == null || showId.trim().isEmpty()) {
            return eventId + "_DEFAULT";
        }
        return showId;
    }
}
