package com.FinalProject.feature_home_organizer.data;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.util.Event_API;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventRepository {

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
        Event_API.getEventsByOrganizer(organizerUid, 20)
                .addOnSuccessListener(snapshot -> callback.onSuccess(map(snapshot)))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
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
