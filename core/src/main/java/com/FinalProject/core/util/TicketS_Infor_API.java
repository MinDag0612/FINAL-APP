package com.FinalProject.core.util;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.FinalProject.core.model.TicketInfor;

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

        db.collection("User_Infor")
                .whereEqualTo("email", userEmail)
                .get()
                .continueWithTask(userResult -> {
                    if (userResult.isSuccessful() && !userResult.getResult().isEmpty()) {
                        String userId = userResult.getResult().getDocuments().get(0).getId();

                        // Trả về task tìm event của user
                        return db.collection("Events")
                                .whereEqualTo("organizer_uid", userId)
                                .whereEqualTo("event_name", eventName)
                                .get();
                    } else {
                        Log.w("Firestore", "User not found: " + userEmail);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .continueWithTask(eventResult -> {
                    if (eventResult.getResult() != null && !eventResult.getResult().isEmpty()) {
                        String eventId = eventResult.getResult().getDocuments().get(0).getId();

                        DocumentReference eventsRef = db.collection("Events").document(eventId);
                        CollectionReference ticketsRef = eventsRef.collection("Tickets_infor");

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

}
