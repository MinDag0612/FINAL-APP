package com.FinalProject.feature_event_detail.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public interface EventDetailApi {

    Task<DocumentSnapshot> getEvent(String eventId);

    Task<QuerySnapshot> getTicketTiers(String eventId);

    Task<QuerySnapshot> getReviews(String eventId);

    Task<DocumentSnapshot> getUserById(String userId);
}
