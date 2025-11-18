package com.FinalProject.feature_review_event.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public interface ReviewEventApi {

    FirebaseUser getCurrentUser();

    Task<DocumentSnapshot> getEvent(String eventId);

    Task<QuerySnapshot> getReviewByUser(String eventId, String userId);

    Task<Void> submitReview(String eventId, Map<String, Object> reviewData);
}
