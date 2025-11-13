package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Seeder giúp tạo dữ liệu mẫu trên Firestore để demo app.
 * Việc ghi dữ liệu sử dụng documentId cố định nhằm hạn chế trùng lặp khi run nhiều lần.
 */
public class Seeder {

    private static final String TAG = "Seeder";
    private static final String SAMPLE_EVENT_ID = "seed_tedxyouth_2024";
    private static final String ORGANIZER_EMAIL = "tonminhdang9@gmail.com";
    private static final String REVIEWER_EMAIL = "523h0011@student.tdtu.edu.vn";

    public static void runSeed() {
        UserInfor_API.getUserInforByEmail(ORGANIZER_EMAIL)
                .addOnSuccessListener(snapshot -> seedEvent(snapshot.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Organizer seed skipped: " + e.getMessage()));
    }

    private static void seedEvent(String organizerUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection(StoreField.EVENTS).document(SAMPLE_EVENT_ID);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put(StoreField.EventFields.EVENT_NAME, "TEDxYouth Saigon 2024");
        eventData.put("event_descrip", "8 câu chuyện truyền cảm hứng từ những người trẻ tiên phong trong giáo dục, nghệ thuật và công nghệ.");
        eventData.put("event_start", "2024-12-15T18:30:00Z");
        eventData.put("event_end", "2024-12-15T22:00:00Z");
        eventData.put("event_type", "Innovation");
        eventData.put("cast", "Bích Phương, CTO VNG");
        eventData.put(StoreField.EventFields.EVENT_LOCATION, "Nhà hát Thành phố, Quận 1");
        eventData.put(StoreField.EventFields.ORGANIZER_UID, organizerUid);
        eventData.put("cover_image", "");

        eventRef.set(eventData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    seedTickets(eventRef);
                    seedReviews(eventRef);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed event", e));
    }

    private static void seedTickets(DocumentReference eventRef) {
        Map<String, Object> general = buildTicket("Ghế phổ thông", 690_000, 200, 150);
        Map<String, Object> vip = buildTicket("Ghế VIP", 1_290_000, 80, 60);
        Map<String, Object> premium = buildTicket("Ghế Premium", 1_890_000, 40, 20);

        eventRef.collection(StoreField.TICKETS_INFOR)
                .document("general")
                .set(general, SetOptions.merge());
        eventRef.collection(StoreField.TICKETS_INFOR)
                .document("vip")
                .set(vip, SetOptions.merge());
        eventRef.collection(StoreField.TICKETS_INFOR)
                .document("premium")
                .set(premium, SetOptions.merge());
    }

    private static Map<String, Object> buildTicket(String label, long price, int quantity, int sold) {
        Map<String, Object> map = new HashMap<>();
        map.put(StoreField.TicketFields.TICKETS_CLASS, label);
        map.put(StoreField.TicketFields.TICKETS_PRICE, price);
        map.put(StoreField.TicketFields.TICKETS_QUANTITY, quantity);
        map.put(StoreField.TicketFields.TICKETS_SOLD, sold);
        return map;
    }

    private static void seedReviews(DocumentReference eventRef) {
        UserInfor_API.getUserInforByEmail(REVIEWER_EMAIL)
                .addOnSuccessListener(snapshot -> writeReview(eventRef, snapshot, "Một tối truyền cảm hứng, networking chất lượng."))
                .addOnFailureListener(e -> Log.w(TAG, "Reviewer seed skipped: " + e.getMessage()));
    }

    private static void writeReview(DocumentReference eventRef, DocumentSnapshot userSnapshot, String comment) {
        if (userSnapshot == null || !userSnapshot.exists()) {
            return;
        }
        Map<String, Object> review = new HashMap<>();
        review.put(StoreField.ReviewFields.UID, userSnapshot.getId());
        review.put(StoreField.ReviewFields.RATE, 5);
        review.put(StoreField.ReviewFields.COMMENT, comment);
        review.put(StoreField.ReviewFields.CREATED_AT, FieldValue.serverTimestamp());

        eventRef.collection(StoreField.REVIEWS)
                .document("seed_review_" + userSnapshot.getId())
                .set(review, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed review", e));
    }
}
