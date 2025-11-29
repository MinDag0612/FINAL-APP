package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.TicketInfor;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

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

    public static Task<QuerySnapshot> getEventsByOrganizer(String organizerUid, int limit){
        int fetchLimit = limit > 0 ? limit : 20;
        return db.collection(StoreField.EVENTS)
                .whereEqualTo(StoreField.EventFields.ORGANIZER_UID, organizerUid)
                .limit(fetchLimit)
                .get();
    }

    public static Task<DocumentReference> addNewEvent(Events eventData){
        return db.collection(StoreField.EVENTS)
                .add(eventData.toMap());
    }

    public static Task<DocumentReference> addNewEvent(Events eventData){
        return db.collection(StoreField.EVENTS)
                .add(eventData.toMap());
    }
    // Lấy tất cả event đang active, sort theo thời gian bắt đầu
    public static Task<QuerySnapshot> getAllEvents() {
        return db.collection(StoreField.EVENTS)
                .orderBy("event_start", Query.Direction.ASCENDING)
                .get();
    }

    // Lấy chi tiết 1 event (dùng cho EventDetail)
    public static Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .get();
    }

    public static Task<QuerySnapshot> getTicketsForEvent(String eventId, int limit) {
        if (limit > 0) {
            return db.collection(StoreField.EVENTS)
                    .document(eventId)
                    .collection(StoreField.TICKETS_INFOR)
                    .limit(limit)
                    .get();
        }
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .get();
    }

    public static Task<Void> updateEventWithTicket(String eventId, Events eventData, TicketInfor ticketInfor) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        DocumentReference eventRef = db.collection(StoreField.EVENTS).document(eventId);

        eventRef.set(eventData.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (ticketInfor == null) {
                        tcs.setResult(null);
                        return;
                    }
                    eventRef.collection(StoreField.TICKETS_INFOR)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot != null && !snapshot.isEmpty()) {
                                    DocumentReference ticketRef = snapshot.getDocuments().get(0).getReference();
                                    ticketRef.set(ticketInfor.toMap(), SetOptions.merge())
                                            .addOnSuccessListener(unused2 -> tcs.setResult(null))
                                            .addOnFailureListener(tcs::setException);
                                } else {
                                    eventRef.collection(StoreField.TICKETS_INFOR)
                                            .add(ticketInfor.toMap())
                                            .addOnSuccessListener(unused2 -> tcs.setResult(null))
                                            .addOnFailureListener(tcs::setException);
                                }
                            })
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    public static Task<Void> addEventWithTicket(Events eventData, com.FinalProject.core.model.TicketInfor ticketInfor){
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        db.collection(StoreField.EVENTS)
                .add(eventData.toMap())
                .addOnSuccessListener(ref -> {
                    if (ticketInfor == null) {
                        tcs.setResult(null);
                        return;
                    }
                    ref.collection(StoreField.TICKETS_INFOR)
                            .add(ticketInfor.toMap())
                            .addOnSuccessListener(ticketRef -> tcs.setResult(null))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }


}
