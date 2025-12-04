package com.FinalProject.feature_home_organizer.data;

import android.util.Log;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.util.Event_API;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventRepository {
    private static final String TAG = "OrganizerEventRepo";

    public interface Callback {
        void onSuccess(List<EventItem> events);
        void onFailure(String message);
    }

    public static class EventItem {
        public final String id;
        public final String name;
        public final String start;
        public final String end;
        public final String location;
        public final String type;

        public EventItem(String id, String name, String start, String end, String location, String type) {
            this.id = id;
            this.name = name;
            this.start = start;
            this.end = end;
            this.location = location;
            this.type = type;
        }
    }

    public void loadEvents(String organizerUid, Callback callback) {
        Log.d(TAG, "loadEvents called with organizerUid: " + organizerUid);
        Event_API.getEventsByOrganizer(organizerUid, 20)
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Primary query success, snapshot size: " + (snapshot != null ? snapshot.size() : "null"));
                    List<EventItem> primary = map(snapshot);
                    Log.d(TAG, "Primary mapped events count: " + primary.size());
                    if (!primary.isEmpty()) {
                        callback.onSuccess(primary);
                    } else {
                        Log.d(TAG, "Primary query empty, trying legacy query");
                        fetchLegacyEvents(organizerUid, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Primary query failed: " + e.getMessage(), e);
                    fetchLegacyEvents(organizerUid, callback);
                });
    }

    private void fetchLegacyEvents(String organizerUid, Callback callback) {
        Log.d(TAG, "Fetching legacy events for organizerUid: " + organizerUid);
        Event_API.getEventsByLegacyUid(organizerUid, 20)
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Legacy query success, snapshot size: " + (snapshot != null ? snapshot.size() : "null"));
                    List<EventItem> events = map(snapshot);
                    Log.d(TAG, "Legacy mapped events count: " + events.size());
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Legacy query failed: " + e.getMessage(), e);
                    callback.onFailure(e.getMessage());
                });
    }

    private List<EventItem> map(QuerySnapshot snapshot) {
        List<EventItem> list = new ArrayList<>();
        if (snapshot == null) return list;
        for (QueryDocumentSnapshot doc : snapshot) {
            String id = doc.getId();
            String name = doc.getString("event_name");
            String start = doc.getString("event_start");
            String end = doc.getString("event_end");
            String location = doc.getString("event_location");
            if (location == null) {
                location = doc.getString("location");
            }
            String type = doc.getString("event_type");
            list.add(new EventItem(id, name, start, end, location, type));
        }
        return list;
    }
}
