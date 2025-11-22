package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.FinalProject.core.model.TicketInfor;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class TicketS_Infor_API {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static void addTicketInfor(
            String userEmail,
            String ticketClass,
            String eventName,
            int ticketQuantity,
            int ticketPrice,
            int ticketsSold
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Khởi tạo ticket
        TicketInfor newTicket = new TicketInfor();
        newTicket.setTickets_class(ticketClass);
        newTicket.setTickets_quantity(ticketQuantity);
        newTicket.setTickets_price(ticketPrice);
        newTicket.setTickets_sold(ticketsSold);

        db.collection(StoreField.USER_INFOR)
                .whereEqualTo(StoreField.UserFields.EMAIL, userEmail)
                .get()
                .continueWithTask(userResult -> {
                    if (userResult.isSuccessful() && !userResult.getResult().isEmpty()) {
                        String userId = userResult.getResult().getDocuments().get(0).getId();

                        // Trả về task tìm event của user
                        return db.collection(StoreField.EVENTS)
                                .whereEqualTo(StoreField.EventFields.ORGANIZER_UID, userId)
                                .whereEqualTo(StoreField.EventFields.EVENT_NAME, eventName)
                                .get();
                    } else {
                        Log.w("Firestore", "User not found: " + userEmail);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .continueWithTask(eventResult -> {
                    if (eventResult.getResult() != null && !eventResult.getResult().isEmpty()) {
                        String eventId = eventResult.getResult().getDocuments().get(0).getId();

                        DocumentReference eventsRef = db.collection(StoreField.EVENTS).document(eventId);
                        CollectionReference ticketsRef = eventsRef.collection(StoreField.TICKETS_INFOR);

                        // Sử dụng eventName + ticketClass làm documentId để tránh trùng
                        String ticketInforId = eventName + " " + ticketClass;

                        return ticketsRef.document(ticketInforId).set(newTicket.toMap());

                    } else {
                        Log.w("Firestore", "Event not found for user: " + userEmail);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .addOnSuccessListener(docRef -> {
                    Log.d("Firestore", "Ticket added successfully");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding ticket", e));
    }

    public static Task<QuerySnapshot> getTicketInforByEventId(String eventId) {
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .orderBy(StoreField.TicketFields.TICKETS_PRICE, Query.Direction.ASCENDING)
                .get();
    }

}
