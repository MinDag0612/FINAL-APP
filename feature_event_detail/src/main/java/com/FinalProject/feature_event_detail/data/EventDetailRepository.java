package com.FinalProject.feature_event_detail.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_event_detail.model.EventDetail;
import com.FinalProject.feature_event_detail.model.ReviewDisplayItem;
import com.FinalProject.feature_event_detail.model.TicketTier;
import com.FinalProject.feature_event_detail.model.TimelineItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
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
 * Repository gom dữ liệu Event detail thông qua {@link EventDetailApi}
 * và chuyển thành model hiển thị.
 */
public class EventDetailRepository {

    public interface Callback {
        void onSuccess(EventDetail detail);
        void onError(String message);
    }

    private final EventDetailApi api;
    private final SimpleDateFormat isoFormatter;
    private final SimpleDateFormat displayTimeFormatter;

    public EventDetailRepository() {
        this(new FirebaseEventDetailApi());
    }

    public EventDetailRepository(EventDetailApi api) {
        this.api = api;
        isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayTimeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void getEventDetail(String eventId, Callback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onError("Thiếu mã sự kiện.");
            return;
        }

        api.getEvent(eventId)
                .addOnSuccessListener(eventSnapshot -> {
                    if (eventSnapshot == null || !eventSnapshot.exists()) {
                        callback.onError("Sự kiện không tồn tại hoặc đã bị xoá.");
                        return;
                    }

                    Task<QuerySnapshot> ticketTask = api.getTicketTiers(eventSnapshot.getId());
                    Task<QuerySnapshot> reviewTask = api.getReviews(eventSnapshot.getId());

                    Tasks.whenAllComplete(ticketTask, reviewTask)
                            .addOnSuccessListener(tasks ->
                                    buildDetail(eventSnapshot, ticketTask, reviewTask, callback))
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
            callback.onSuccess(createDetail(eventSnapshot, ticketTiers,
                    Collections.emptyList(), 0, 0));
            return;
        }

        fetchReviewerNames(reviewsSnapshot)
                .addOnSuccessListener(userMap -> {
                    List<ReviewDisplayItem> reviewItems = mapReviews(reviewsSnapshot, userMap);
                    callback.onSuccess(createDetail(
                            eventSnapshot,
                            ticketTiers,
                            reviewItems,
                            calculateAverage(reviewItems),
                            reviewItems.size()
                    ));
                })
                .addOnFailureListener(e -> {
                    List<ReviewDisplayItem> reviewItems = mapReviews(reviewsSnapshot, Collections.emptyMap());
                    callback.onSuccess(createDetail(
                            eventSnapshot,
                            ticketTiers,
                            reviewItems,
                            calculateAverage(reviewItems),
                            reviewItems.size()
                    ));
                });
    }

    private EventDetail createDetail(DocumentSnapshot eventSnapshot,
                                     List<TicketTier> ticketTiers,
                                     List<ReviewDisplayItem> reviews,
                                     float averageRating,
                                     int totalReview) {
        return new EventDetail(
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
                reviews,
                averageRating,
                totalReview
        );
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
            userTasks.add(api.getUserById(userId));
        }

        return Tasks.whenAllSuccess(userTasks)
                .continueWith(task -> {
                    Map<String, String> map = new HashMap<>();
                    List<?> results = task.getResult();
                    if (results != null) {
                        for (Object obj : results) {
                            if (obj instanceof DocumentSnapshot) {
                                DocumentSnapshot doc = (DocumentSnapshot) obj;
                                map.put(doc.getId(),
                                        doc.getString(StoreField.UserFields.FULLNAME));
                            }
                        }
                    }
                    return map;
                });
    }

    private List<ReviewDisplayItem> mapReviews(QuerySnapshot reviewsSnapshot,
                                               Map<String, String> userMap) {
        if (reviewsSnapshot == null || reviewsSnapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewDisplayItem> items = new ArrayList<>();
        for (DocumentSnapshot reviewDoc : reviewsSnapshot) {
            String uid = reviewDoc.getString(StoreField.ReviewFields.UID);
            Double rate = reviewDoc.getDouble(StoreField.ReviewFields.RATE);
            String comment = reviewDoc.getString(StoreField.ReviewFields.COMMENT);
            items.add(new ReviewDisplayItem(
                    userMap.getOrDefault(uid, "Ẩn danh"),
                    rate != null ? (int) Math.round(rate) : 0,
                    comment
            ));
        }
        return items;
    }

    private float calculateAverage(List<ReviewDisplayItem> reviews) {
        if (reviews.isEmpty()) {
            return 0f;
        }
        float total = 0f;
        for (ReviewDisplayItem item : reviews) {
            total += item.getRating();
        }
        return total / reviews.size();
    }

    private List<TimelineItem> buildTimeline(String startIso, String endIso) {
        List<TimelineItem> items = new ArrayList<>();
        if (TextUtils.isEmpty(startIso) || TextUtils.isEmpty(endIso)) {
            return items;
        }
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(isoFormatter.parse(startIso));

            Calendar gateCal = (Calendar) startCal.clone();
            gateCal.add(Calendar.HOUR_OF_DAY, -1);

            items.add(new TimelineItem(
                    displayTimeFormatter.format(gateCal.getTime()),
                    "Mở cửa đón khách",
                    "Kiểm tra vé và nhận vòng tay"
            ));

            items.add(new TimelineItem(
                    displayTimeFormatter.format(startCal.getTime()),
                    "Bắt đầu chương trình",
                    "Trải nghiệm tiết mục chính"
            ));

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(isoFormatter.parse(endIso));
            items.add(new TimelineItem(
                    displayTimeFormatter.format(endCal.getTime()),
                    "Kết thúc",
                    "Khu vực chụp hình & meet-and-greet"
            ));
        } catch (Exception ignored) {
        }
        return items;
    }

    private String value(String raw, String fallback) {
        return TextUtils.isEmpty(raw) ? fallback : raw;
    }
}
