package com.FinalProject.core.util;

import android.util.Log;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.FinalProject.core.model.Events;

public class Event_API {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void addEventForUser(
            String emailUser,
            String eventName,
            String eventDescrip,
            String eventStart,
            String eventEnd,
            String cast,
            String location,
            String eventType
    ) {
        Events eventData = new Events();

        db.collection("User_Infor")
                .whereEqualTo("email", emailUser)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) result.getDocuments().get(0);
                        String uid = document.getId();

                        // Set dữ liệu event
                        eventData.setUid(uid);
                        eventData.setEvent_name(eventName);
                        eventData.setEvent_descrip(eventDescrip);
                        eventData.setEvent_start(eventStart);
                        eventData.setEvent_end(eventEnd);
                        eventData.setCast(cast);
                        eventData.setLocation(location);
                        eventData.setEvent_type(eventType);

                        // Thêm vào Firestore
                        db.collection("Events")
                                .add(eventData.toMap())
                                .addOnSuccessListener(ref -> {
                                    Log.d("Firestore", "Event created with ID: " + ref.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error adding event", e);
                                });
                    } else {
                        Log.w("Firestore", "User not found for email: " + emailUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching user", e);
                });
    }




}