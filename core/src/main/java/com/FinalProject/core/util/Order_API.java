package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.FinalProject.core.model.TicketItem;
import com.FinalProject.core.model.Orders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Order_API {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void addOrder(
            String userEmail,
            String eventName,
            String ticketClass,
            int orderQuantity,
            String paymentMethod
    ) {
        AtomicReference<String> userRef = new AtomicReference<>();
        AtomicReference<String> eventRef = new AtomicReference<>();

        db.collection(StoreField.USER_INFOR)
                .whereEqualTo(StoreField.UserFields.EMAIL, userEmail)
                .get()
                .continueWithTask(userResult -> {
                    if (userResult.isSuccessful() && !userResult.getResult().isEmpty()) {
                        userRef.set(userResult.getResult().getDocuments().get(0).getId());

                        return db.collection(StoreField.EVENTS)
                                .whereEqualTo(StoreField.EventFields.EVENT_NAME, eventName)
                                .get();
                    } else {
                        Log.w("Firestore", "User not found: " + userEmail);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .continueWithTask(eventResult -> {
                    if (eventResult.isSuccessful() && !eventResult.getResult().isEmpty()) {
                        String eventId = eventResult.getResult().getDocuments().get(0).getId();
                        eventRef.set(eventId);

                        return db.collection(StoreField.EVENTS)
                                .document(eventId)
                                .collection(StoreField.TICKETS_INFOR)
                                .whereEqualTo(StoreField.TicketFields.TICKETS_CLASS, ticketClass)
                                .get();
                    } else {
                        Log.w("Firestore", "Event not found: " + eventName);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .continueWithTask(ticketResult -> {
                    if (ticketResult.isSuccessful() && !ticketResult.getResult().isEmpty()) {
                        QuerySnapshot querySnapshot = ticketResult.getResult();
                        String ticketId = querySnapshot.getDocuments().get(0).getId();
                        int eachPrice = querySnapshot.getDocuments().get(0).getLong(StoreField.TicketFields.TICKETS_CLASS).intValue();

                        int totalPrice = orderQuantity * eachPrice;
                        String userId = userRef.get();
                        String eventId = eventRef.get();

                        TicketItem ticketItem = new TicketItem(ticketId, orderQuantity);
                        List<TicketItem> ticketItems = new ArrayList<>();
                        ticketItems.add(ticketItem);

                        Orders newOrder = new Orders(userId, totalPrice, false, ticketItems, paymentMethod);
                        return db.collection(StoreField.ORDERS)
                                .add(newOrder);
                    } else {
                        Log.w("Firestore", "Ticket info not found for event: " + eventName);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                })
                .addOnSuccessListener(docRef -> {
                    Log.d("Firestore", "Order added successfully");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding order", e));
    }

}
