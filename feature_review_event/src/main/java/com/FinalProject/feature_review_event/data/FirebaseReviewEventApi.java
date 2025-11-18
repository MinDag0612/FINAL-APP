package com.FinalProject.feature_review_event.data;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class FirebaseReviewEventApi implements ReviewEventApi {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseReviewEventApi() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    public FirebaseReviewEventApi(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @Override
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .get();
    }

    @Override
    public Task<QuerySnapshot> getReviewByUser(String eventId, String userId) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.REVIEWS)
                .whereEqualTo(StoreField.ReviewFields.UID, userId)
                .limit(1)
                .get();
    }

    @Override
    public Task<Void> submitReview(String eventId, Map<String, Object> reviewData) {
        return firestore.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.REVIEWS)
                .add(reviewData)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference ignored = task.getResult();
                        return Tasks.forResult(null);
                    } else if (task.getException() != null) {
                        return Tasks.forException(task.getException());
                    } else {
                        return Tasks.forException(new IllegalStateException("Không thể lưu review."));
                    }
                });
    }
}
