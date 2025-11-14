package com.FinalProject.feature_event_detail.data;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseEventDetailApi implements EventDetailApi {

    private final FirebaseFirestore firestore;

    public FirebaseEventDetailApi() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseEventDetailApi(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .get();
    }

    @Override
    public Task<QuerySnapshot> getTicketTiers(String eventId) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .orderBy(StoreField.TicketFields.TICKETS_PRICE, Query.Direction.ASCENDING)
                .get();
    }

    @Override
    public Task<QuerySnapshot> getReviews(String eventId) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.REVIEWS)
                .get();
    }

    @Override
    public Task<DocumentSnapshot> getUserById(String userId) {
        return firestore.collection(StoreField.USER_INFOR)
                .document(userId)
                .get();
    }
}
