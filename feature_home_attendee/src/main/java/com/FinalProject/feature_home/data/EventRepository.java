package com.FinalProject.feature_home.data;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.feature_home.model.HomeEvent;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyển dữ liệu Firestore -> HomeEvent, mọi truy vấn vé thông qua {@link HomeApi}.
 */
public class EventRepository {

    private final HomeApi api;

    public EventRepository(HomeApi api) {
        this.api = api;
    }

    public Task<List<HomeEvent>> buildEvents(QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return Tasks.forResult(Collections.emptyList());
        }

        List<Task<HomeEvent>> eventTasks = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot) {
            eventTasks.add(mapEvent(document));
        }

        return Tasks.whenAllSuccess(eventTasks)
                .continueWith(task -> {
                    List<HomeEvent> mappedEvents = new ArrayList<>();
                    List<?> result = task.getResult();
                    if (result != null) {
                        for (Object item : result) {
                            if (item instanceof HomeEvent) {
                                mappedEvents.add((HomeEvent) item);
                            }
                        }
                    }
                    return mappedEvents;
                });
    }

    private Task<HomeEvent> mapEvent(DocumentSnapshot documentSnapshot) {
        String id = documentSnapshot.getId();
        String name = documentSnapshot.getString(StoreField.EventFields.EVENT_NAME);
        String description = documentSnapshot.getString("event_descrip");
        String location = documentSnapshot.getString(StoreField.EventFields.EVENT_LOCATION);
        String eventType = documentSnapshot.getString("event_type");
        String startTime = documentSnapshot.getString("event_start");
        String cast = documentSnapshot.getString("cast");

        HomeEvent event = new HomeEvent(
                id,
                name != null ? name : "Sự kiện đặc biệt",
                description != null ? description : "",
                location != null ? location : "Đang cập nhật",
                eventType != null ? eventType : "Khác",
                startTime,
                cast
        );

        return api.getTicketsForEvent(id)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot ticketDoc = task.getResult().getDocuments().get(0);
                        Long price = ticketDoc.getLong(StoreField.TicketFields.TICKETS_PRICE);
                        if (price != null) {
                            event.setStartingPrice(price);
                        }
                    }
                    return event;
                });
    }



}
