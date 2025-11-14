package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.FinalProject.core.model.Events;
import com.google.firebase.firestore.QuerySnapshot;

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

        db.collection(StoreField.USER_INFOR)
                .whereEqualTo(StoreField.UserFields.EMAIL, emailUser)
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
                        db.collection(StoreField.EVENTS)
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

    public static Task<QuerySnapshot> getEventASC(int limit){
        int fetchLimit = limit > 0 ? limit : 10;
        return db.collection(StoreField.EVENTS)
                .orderBy("event_start", Query.Direction.ASCENDING)
                .limit(fetchLimit)
                .get();
    }




}
