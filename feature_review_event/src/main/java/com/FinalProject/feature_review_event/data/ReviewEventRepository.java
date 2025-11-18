package com.FinalProject.feature_review_event.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_review_event.model.ReviewEventContent;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ReviewEventRepository {

    public interface ReviewContentCallback {
        void onSuccess(ReviewEventContent content);
        void onError(String message);
    }

    public interface SubmitCallback {
        void onSuccess();
        void onError(String message);
    }

    private final ReviewEventApi api;
    private final SimpleDateFormat isoFormatter;
    private final SimpleDateFormat displayDateFormatter;
    private final SimpleDateFormat displayTimeFormatter;

    public ReviewEventRepository() {
        this(new FirebaseReviewEventApi());
    }

    public ReviewEventRepository(@NonNull ReviewEventApi api) {
        this.api = api;
        isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormatter = new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("vi", "VN"));
        displayTimeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void loadReviewContent(String eventId, ReviewContentCallback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onError("Thiếu mã sự kiện để tải thông tin.");
            return;
        }
        FirebaseUser currentUser = api.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn cần đăng nhập để đánh giá sự kiện.");
            return;
        }

        Task<DocumentSnapshot> eventTask = api.getEvent(eventId);
        Task<QuerySnapshot> reviewTask = api.getReviewByUser(eventId, currentUser.getUid());

        Tasks.whenAllComplete(eventTask, reviewTask)
                .addOnSuccessListener(tasks -> {
                    if (!eventTask.isSuccessful()) {
                        callback.onError(resolveError(eventTask.getException()));
                        return;
                    }
                    DocumentSnapshot eventSnapshot = eventTask.getResult();
                    if (eventSnapshot == null || !eventSnapshot.exists()) {
                        callback.onError("Sự kiện không tồn tại hoặc đã bị xoá.");
                        return;
                    }
                    boolean alreadyReviewed = reviewTask.isSuccessful()
                            && reviewTask.getResult() != null
                            && !reviewTask.getResult().isEmpty();
                    callback.onSuccess(mapContent(eventSnapshot, alreadyReviewed));
                })
                .addOnFailureListener(e -> callback.onError(resolveError(e)));
    }

    public void submitReview(String eventId, int rating, String comment, SubmitCallback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onError("Thiếu mã sự kiện.");
            return;
        }
        FirebaseUser currentUser = api.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn cần đăng nhập để gửi đánh giá.");
            return;
        }
        if (rating < 1 || rating > 5) {
            callback.onError("Vui lòng chọn số sao hợp lệ.");
            return;
        }
        String safeComment = comment != null ? comment.trim() : "";
        if (safeComment.length() < 10) {
            callback.onError("Vui lòng nhập nhận xét tối thiểu 10 ký tự.");
            return;
        }

        api.getReviewByUser(eventId, currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        callback.onError("Bạn đã gửi đánh giá cho sự kiện này.");
                        return;
                    }
                    Map<String, Object> reviewData = new HashMap<>();
                    reviewData.put(StoreField.ReviewFields.UID, currentUser.getUid());
                    reviewData.put(StoreField.ReviewFields.RATE, rating);
                    reviewData.put(StoreField.ReviewFields.COMMENT, safeComment);
                    reviewData.put(StoreField.ReviewFields.CREATED_AT, FieldValue.serverTimestamp());

                    api.submitReview(eventId, reviewData)
                            .addOnSuccessListener(ignored -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(resolveError(e)));
                })
                .addOnFailureListener(e -> callback.onError(resolveError(e)));
    }

    private ReviewEventContent mapContent(DocumentSnapshot snapshot, boolean alreadyReviewed) {
        String name = snapshot.getString(StoreField.EventFields.EVENT_NAME);
        String schedule = formatSchedule(
                snapshot.getString("event_start"),
                snapshot.getString("event_end")
        );
        String location = snapshot.getString(StoreField.EventFields.EVENT_LOCATION);
        return new ReviewEventContent(
                fallback(name, "Sự kiện đặc biệt"),
                fallback(schedule, "Đang cập nhật"),
                fallback(location, "Đang cập nhật"),
                alreadyReviewed
        );
    }

    private String formatSchedule(String startIso, String endIso) {
        if (TextUtils.isEmpty(startIso)) {
            return "";
        }
        try {
            Date start = isoFormatter.parse(startIso);
            if (start == null) {
                return startIso;
            }
            String display = displayDateFormatter.format(start);
            if (!TextUtils.isEmpty(endIso)) {
                Date end = isoFormatter.parse(endIso);
                if (end != null) {
                    display = display + " - " + displayTimeFormatter.format(end);
                }
            }
            return display;
        } catch (Exception e) {
            return startIso;
        }
    }

    private String fallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String resolveError(Exception e) {
        if (e == null || TextUtils.isEmpty(e.getMessage())) {
            return "Đã có lỗi xảy ra. Vui lòng thử lại.";
        }
        return e.getMessage();
    }
}
