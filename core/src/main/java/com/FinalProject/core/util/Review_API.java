package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.model.Review;
import com.google.firebase.firestore.FirebaseFirestore;
import com.FinalProject.core.constName.StoreField;

import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

public class Review_API {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void addReview(
            String eventName,
            String emailUser,
            int rate,
            String comment
    ){
        AtomicReference<String> userRef = new AtomicReference<>();
        AtomicReference<String> eventRef = new AtomicReference<>();

        db.collection(StoreField.EVENTS)
                .whereEqualTo(StoreField.EventFields.EVENT_NAME, eventName)
                .get()
                .continueWithTask(eventResult -> {
                    if (eventResult.isSuccessful() && !eventResult.getResult().isEmpty()){
                        String eventId = eventResult.getResult().getDocuments().get(0).getId();
                        eventRef.set(eventId);

                        return db.collection(StoreField.USER_INFOR)
                                .whereEqualTo(StoreField.UserFields.EMAIL, emailUser)
                                .get();
                    }
                    else {
                        throw new Exception("Event not found");
                    }
                })
                .continueWithTask(userResult -> {
                    if (userResult.isSuccessful() && !userResult.getResult().isEmpty()){
                        String userId = userResult.getResult().getDocuments().get(0).getId();

                        Review newReview = new Review(userId, rate, comment);

                        return db.collection(StoreField.EVENTS)
                                .document(eventRef.get())
                                .collection(StoreField.REVIEWS)
                                .add(newReview.toMap());
                    }
                    else {
                        throw new Exception("User not found");
                    }
                })
                .addOnSuccessListener(docRef -> {
                    Log.d("Firestore", "Review added successfully");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding Review", e));
    }


}
