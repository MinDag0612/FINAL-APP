package com.FinalProject.feature_event_detail.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_event_detail.model.EventDetail;
import com.FinalProject.feature_event_detail.model.ReviewDisplayItem;
import com.FinalProject.feature_event_detail.model.TicketTier;
import com.FinalProject.feature_event_detail.model.TimelineItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Repository chịu trách nhiệm gom dữ liệu chi tiết từ Firestore.
 */
public class EventDetailRepository {

    public interface Callback {
        void onSuccess(EventDetail detail);
        void onError(String message);
    }

    private final FirebaseFirestore firestore;
    private final SimpleDateFormat isoFormatter;
    private final SimpleDateFormat displayTimeFormatter;

    public EventDetailRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public EventDetailRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
        isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayTimeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void getEventDetail(String eventId, Callback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onError("Thiếu mã sự kiện.");
            return;
        }

        DocumentReference eventRef = firestore.collection(StoreField.EVENTS).document(eventId);
        eventRef.get()
                .addOnSuccessListener(eventSnapshot -> {
                    if (eventSnapshot == null || !eventSnapshot.exists()) {
                        callback.onError("Sự kiện không tồn tại hoặc đã bị xoá.");
                        return;
                    }
                    Task<QuerySnapshot> ticketTask = eventRef.collection(StoreField.TICKETS_INFOR)
                            .orderBy(StoreField.TicketFields.TICKETS_PRICE, Query.Direction.ASCENDING)
                            .get();
                    Task<QuerySnapshot> reviewTask = eventRef.collection(StoreField.REVIEWS).get();

                    Tasks.whenAllComplete(ticketTask, reviewTask)
                            .addOnSuccessListener(tasks -> buildDetail(eventSnapshot, ticketTask, reviewTask, callback))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void buildDetail(DocumentSnapshot eventSnapshot,
                             Task<QuerySnapshot> ticketTask,
                             Task<QuerySnapshot> reviewTask,
                             Callback callback) {

        QuerySnapshot ticketsSnapshot = ticketTask.isSuccessful() ? ticketTask.getResult() : null;
        QuerySnapshot reviewsSnapshot = reviewTask.isSuccessful() ? reviewTask.getResult() : null;

        List<TicketTier> ticketTiers = mapTickets(ticketsSnapshot);

        if (reviewsSnapshot == null || reviewsSnapshot.isEmpty()) {
            EventDetail detail = new EventDetail(
                    eventSnapshot.getId(),
                    value(eventSnapshot.getString(StoreField.EventFields.EVENT_NAME), "Sự kiện bất kỳ"),
                    eventSnapshot.getString("event_descrip"),
                    eventSnapshot.getString(StoreField.EventFields.EVENT_LOCATION),
                    eventSnapshot.getString("event_type"),
                    eventSnapshot.getString("event_start"),
                    eventSnapshot.getString("event_end"),
                    eventSnapshot.getString("cast"),
                    eventSnapshot.getString("cover_image"),
                    buildTags(eventSnapshot),
                    ticketTiers,
                    buildTimeline(eventSnapshot.getString("event_start"), eventSnapshot.getString("event_end")),
                    Collections.emptyList(),
                    0,
                    0
            );
            callback.onSuccess(detail);
            return;
        }

        fetchReviewerNames(reviewsSnapshot)
                .addOnSuccessListener(userMap -> {
                    List<ReviewDisplayItem> reviewItems = mapReviews(reviewsSnapshot, userMap);
                    EventDetail detail = new EventDetail(
                            eventSnapshot.getId(),
                            value(eventSnapshot.getString(StoreField.EventFields.EVENT_NAME), "Sự kiện bất kỳ"),
                            eventSnapshot.getString("event_descrip"),
                            eventSnapshot.getString(StoreField.EventFields.EVENT_LOCATION),
                            eventSnapshot.getString("event_type"),
                            eventSnapshot.getString("event_start"),
                            eventSnapshot.getString("event_end"),
                            eventSnapshot.getString("cast"),
                            eventSnapshot.getString("cover_image"),
                            buildTags(eventSnapshot),
                            ticketTiers,
                            buildTimeline(eventSnapshot.getString("event_start"), eventSnapshot.getString("event_end")),
                            reviewItems,
                            calculateAverage(reviewItems),
                            reviewItems.size()
                    );
                    callback.onSuccess(detail);
                })
                .addOnFailureListener(e -> {
                    List<ReviewDisplayItem> reviewItems = mapReviews(reviewsSnapshot, Collections.emptyMap());
                    EventDetail detail = new EventDetail(
                            eventSnapshot.getId(),
                            value(eventSnapshot.getString(StoreField.EventFields.EVENT_NAME), "Sự kiện bất kỳ"),
                            eventSnapshot.getString("event_descrip"),
                            eventSnapshot.getString(StoreField.EventFields.EVENT_LOCATION),
                            eventSnapshot.getString("event_type"),
                            eventSnapshot.getString("event_start"),
                            eventSnapshot.getString("event_end"),
                            eventSnapshot.getString("cast"),
                            eventSnapshot.getString("cover_image"),
                            buildTags(eventSnapshot),
                            ticketTiers,
                            buildTimeline(eventSnapshot.getString("event_start"), eventSnapshot.getString("event_end")),
                            reviewItems,
                            calculateAverage(reviewItems),
                            reviewItems.size()
                    );
                    callback.onSuccess(detail);
                });
    }

    private List<String> buildTags(DocumentSnapshot eventSnapshot) {
        List<String> tags = new ArrayList<>();
        String cast = eventSnapshot.getString("cast");
        String eventType = eventSnapshot.getString("event_type");
        String location = eventSnapshot.getString(StoreField.EventFields.EVENT_LOCATION);
        if (!TextUtils.isEmpty(eventType)) {
            tags.add(eventType);
        }
        if (!TextUtils.isEmpty(location)) {
            tags.add(location);
        }
        if (!TextUtils.isEmpty(cast)) {
            tags.add("Khách mời: " + cast);
        }
        return tags;
    }

    private List<TicketTier> mapTickets(QuerySnapshot ticketsSnapshot) {
        if (ticketsSnapshot == null || ticketsSnapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<TicketTier> tiers = new ArrayList<>();
        for (DocumentSnapshot ticketDoc : ticketsSnapshot) {
            String label = ticketDoc.getString(StoreField.TicketFields.TICKETS_CLASS);
            Long price = ticketDoc.getLong(StoreField.TicketFields.TICKETS_PRICE);
            Long quantity = ticketDoc.getLong(StoreField.TicketFields.TICKETS_QUANTITY);
            Long sold = ticketDoc.getLong(StoreField.TicketFields.TICKETS_SOLD);
            tiers.add(new TicketTier(
                    value(label, "Standard"),
                    price != null ? price : 0,
                    quantity != null ? quantity.intValue() : 0,
                    sold != null ? sold.intValue() : 0
            ));
        }
        return tiers;
    }

    private Task<Map<String, String>> fetchReviewerNames(QuerySnapshot reviewSnapshot) {
        Set<String> userIds = new HashSet<>();
        for (QueryDocumentSnapshot reviewDoc : reviewSnapshot) {
            String uid = reviewDoc.getString(StoreField.ReviewFields.UID);
            if (!TextUtils.isEmpty(uid)) {
                userIds.add(uid);
            }
        }
        if (userIds.isEmpty()) {
            return Tasks.forResult(Collections.emptyMap());
        }
        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
        for (String userId : userIds) {
            userTasks.add(firestore.collection(StoreField.USER_INFOR).document(userId).get());
        }

        TaskCompletionSource<Map<String, String>> tcs = new TaskCompletionSource<>();
        Tasks.whenAllComplete(userTasks)
                .addOnSuccessListener(tasks -> {
                    Map<String, String> nameMap = new HashMap<>();
                    for (Task<DocumentSnapshot> userTask : userTasks) {
                        if (userTask.isSuccessful()) {
                            DocumentSnapshot snapshot = userTask.getResult();
                            if (snapshot != null && snapshot.exists()) {
                                nameMap.put(snapshot.getId(),
                                        value(snapshot.getString(StoreField.UserFields.FULLNAME), "Ẩn danh"));
                            }
                        }
                    }
                    tcs.setResult(nameMap);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    private List<ReviewDisplayItem> mapReviews(QuerySnapshot reviewSnapshot,
                                               Map<String, String> userNameMap) {
        if (reviewSnapshot == null || reviewSnapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewDisplayItem> reviews = new ArrayList<>();
        for (QueryDocumentSnapshot reviewDoc : reviewSnapshot) {
            String uid = reviewDoc.getString(StoreField.ReviewFields.UID);
            Long rate = reviewDoc.getLong(StoreField.ReviewFields.RATE);
            String comment = reviewDoc.getString(StoreField.ReviewFields.COMMENT);
            String reviewerName = userNameMap.getOrDefault(uid, "Ẩn danh");
            reviews.add(new ReviewDisplayItem(
                    reviewerName,
                    rate != null ? rate.intValue() : 0,
                    value(comment, "Không có bình luận")
            ));
        }
        return reviews;
    }

    private List<TimelineItem> buildTimeline(String startIso, String endIso) {
        List<TimelineItem> timelineItems = new ArrayList<>();
        try {
            if (!TextUtils.isEmpty(startIso)) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(isoFormatter.parse(startIso));

                Calendar checkIn = (Calendar) calendar.clone();
                checkIn.add(Calendar.MINUTE, -30);
                timelineItems.add(new TimelineItem(
                        displayTimeFormatter.format(checkIn.getTime()),
                        "Check-in & Welcome drink",
                        "Đăng ký, nhận badge và giao lưu nhẹ."
                ));

                timelineItems.add(new TimelineItem(
                        displayTimeFormatter.format(calendar.getTime()),
                        "Khai mạc & Phiên 1",
                        "Giới thiệu chương trình, chủ đề và diễn giả chính."
                ));

                Calendar networking = (Calendar) calendar.clone();
                networking.add(Calendar.HOUR_OF_DAY, 2);
                timelineItems.add(new TimelineItem(
                        displayTimeFormatter.format(networking.getTime()),
                        "Networking chuyên sâu",
                        "Khu vực VIP cùng đồ uống signature."
                ));
            }
        } catch (Exception ignored) {
            timelineItems.add(new TimelineItem("18:00", "Đón khách", "Check-in &amp; Welcome drink"));
            timelineItems.add(new TimelineItem("19:00", "Phiên chính", "Chia sẻ câu chuyện cảm hứng"));
            timelineItems.add(new TimelineItem("21:00", "Gặp gỡ VIP", "Giao lưu diễn giả & ký tặng"));
        }
        return timelineItems;
    }

    private double calculateAverage(List<ReviewDisplayItem> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (ReviewDisplayItem item : reviews) {
            total += item.getRating();
        }
        return total / reviews.size();
    }

    private String value(String origin, String fallback) {
        return origin == null || origin.isEmpty() ? fallback : origin;
    }
}
