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
    private static final String EXTRA_EVENT_LOCATION = "eventLocation";
    private static final String EXTRA_EVENT_SCHEDULE = "eventSchedule";
    private static final String EXTRA_EVENT_PRICE = "eventPrice";

    /**
     * @param eventId   required event identifier
     * @param eventTitle optional name/title, used for toolbar
     * @param location   optional fallback location shown immediately in booking header
     * @param schedule   optional fallback schedule shown immediately in booking header
     * @param price      optional starting price used to populate the price label
     * @param showId     optional show/seating session id; will default to eventId + "_DEFAULT" if missing
     */
    public static Intent createBookingIntent(Context context,
                                             String eventId,
                                             String eventTitle,
                                             String location,
                                             String schedule,
                                             long price,
                                             String showId) {
        Intent intent = new Intent(context, BookingHostActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_EVENT_TITLE, eventTitle);
        intent.putExtra(EXTRA_EVENT_LOCATION, location);
        intent.putExtra(EXTRA_EVENT_SCHEDULE, schedule);
        intent.putExtra(EXTRA_EVENT_PRICE, price);
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
