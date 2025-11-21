package com.FinalProject.feature_create_event.data;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Event_API;

public class CreateEventRepositoryImpl {
    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public void createEventWithTicket(Events event, TicketInfor ticketInfor, Callback callback) {
        Event_API.addEventWithTicket(event, ticketInfor)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
