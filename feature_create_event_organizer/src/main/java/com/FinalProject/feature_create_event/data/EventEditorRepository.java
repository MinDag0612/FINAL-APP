package com.FinalProject.feature_create_event.data;

import androidx.annotation.Nullable;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Event_API;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class EventEditorRepository {

    public static class EventWithTicket {
        public final Events event;
        @Nullable public final TicketInfor ticket;
        @Nullable public final String ticketDocId;

        public EventWithTicket(Events event, @Nullable TicketInfor ticket, @Nullable String ticketDocId) {
            this.event = event;
            this.ticket = ticket;
            this.ticketDocId = ticketDocId;
        }
    }

    public interface LoadCallback {
        void onSuccess(EventWithTicket data);
        void onFailure(String message);
    }

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public void loadEvent(String eventId, LoadCallback callback) {
        Tasks.whenAllSuccess(Event_API.getEventById(eventId), Event_API.getTicketsForEvent(eventId, 1))
                .addOnSuccessListener(results -> {
                    DocumentSnapshot eventSnap = (DocumentSnapshot) results.get(0);
                    QuerySnapshot ticketSnap = (QuerySnapshot) results.get(1);

                    if (eventSnap == null || !eventSnap.exists()) {
                        callback.onFailure("Không tìm thấy sự kiện");
                        return;
                    }

                    Events event = mapEvent(eventSnap);
                    TicketInfor ticket = null;
                    String ticketDocId = null;
                    if (ticketSnap != null && !ticketSnap.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) ticketSnap.getDocuments().get(0);
                        ticket = mapTicket(doc);
                        ticketDocId = doc.getId();
                    }

                    callback.onSuccess(new EventWithTicket(event, ticket, ticketDocId));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateEvent(String eventId, Events event, TicketInfor ticketInfor, Callback callback) {
        Event_API.updateEventWithTicket(eventId, event, ticketInfor)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    private Events mapEvent(DocumentSnapshot snap) {
        Events e = new Events();
        e.setEvent_name(snap.getString("event_name"));
        e.setEvent_descrip(snap.getString("event_descrip"));
        e.setEvent_start(snap.getString("event_start"));
        e.setEvent_end(snap.getString("event_end"));
        e.setCast(snap.getString("cast"));
        e.setLocation(snap.getString(StoreField.EventFields.EVENT_LOCATION));
        e.setEvent_type(snap.getString("event_type"));
        Long basePrice = snap.getLong("base_price");
        if (basePrice != null) {
            e.setBase_price(basePrice.intValue());
        }
        return e;
    }

    private TicketInfor mapTicket(QueryDocumentSnapshot doc) {
        TicketInfor t = new TicketInfor();
        t.setTickets_class(doc.getString(StoreField.TicketFields.TICKETS_CLASS));
        Long price = doc.getLong(StoreField.TicketFields.TICKETS_PRICE);
        Long qty = doc.getLong(StoreField.TicketFields.TICKETS_QUANTITY);
        Long sold = doc.getLong(StoreField.TicketFields.TICKETS_SOLD);
        t.setTickets_price(price != null ? price.intValue() : 0);
        t.setTickets_quantity(qty != null ? qty.intValue() : 0);
        t.setTickets_sold(sold != null ? sold.intValue() : 0);
        return t;
    }
}
