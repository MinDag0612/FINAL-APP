package com.FinalProject.feature_home.data;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.UserInfor_API;
import com.FinalProject.feature_home.model.HomeArtist;
import com.FinalProject.feature_home.model.HomeContent;
import com.FinalProject.feature_home.model.HomeEvent;
import com.FinalProject.feature_home.model.HomeUser;
import com.FinalProject.feature_home.model.RecentTicketInfo;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeRepository {

    public interface HomeDataCallback {
        void onSuccess(HomeContent content);
        void onError(String message);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public HomeRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    public HomeRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    public void loadHomeContent(HomeDataCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        String email = currentUser.getEmail();
        UserInfor_API.getUserInforByEmail(email)
                .addOnSuccessListener(userSnapshot -> handleUserSnapshot(userSnapshot, email, callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void handleUserSnapshot(DocumentSnapshot userSnapshot,
                                    String email,
                                    HomeDataCallback callback) {
        if (userSnapshot == null || !userSnapshot.exists()) {
            callback.onError("Không tìm thấy thông tin người dùng.");
            return;
        }

        String userId = userSnapshot.getId();
        HomeUser homeUser = new HomeUser(
                userSnapshot.getString(StoreField.UserFields.FULLNAME),
                email,
                userSnapshot.getString(StoreField.UserFields.ROLE)
        );

        firestore.collection(StoreField.EVENTS)
                .orderBy("event_start", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(eventsSnapshot ->
                        buildHomeContent(userId, homeUser, eventsSnapshot, callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void buildHomeContent(String userId,
                                  HomeUser homeUser,
                                  QuerySnapshot eventsSnapshot,
                                  HomeDataCallback callback) {
        buildEvents(eventsSnapshot)
                .addOnSuccessListener(events -> fetchRecentTicket(userId, homeUser, events, callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private Task<List<HomeEvent>> buildEvents(QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return Tasks.forResult(Collections.emptyList());
        }
        List<Task<HomeEvent>> eventTasks = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot) {
            eventTasks.add(mapEvent(document));
        }
        return Tasks.whenAllSuccess(eventTasks)
                .continueWith(task -> {
                    List<HomeEvent> mappedEvents = new ArrayList<>();
                    List<?> result = task.getResult();
                    if (result != null) {
                        for (Object item : result) {
                            if (item instanceof HomeEvent) {
                                mappedEvents.add((HomeEvent) item);
                            }
                        }
                    }
                    return mappedEvents;
                });
    }

    private Task<HomeEvent> mapEvent(DocumentSnapshot documentSnapshot) {
        String id = documentSnapshot.getId();
        String name = documentSnapshot.getString(StoreField.EventFields.EVENT_NAME);
        String description = documentSnapshot.getString("event_descrip");
        String location = documentSnapshot.getString(StoreField.EventFields.EVENT_LOCATION);
        String eventType = documentSnapshot.getString("event_type");
        String startTime = documentSnapshot.getString("event_start");
        String cast = documentSnapshot.getString("cast");

        HomeEvent event = new HomeEvent(
                id,
                name != null ? name : "Sự kiện đặc biệt",
                description != null ? description : "",
                location != null ? location : "Đang cập nhật",
                eventType != null ? eventType : "Khác",
                startTime,
                cast
        );

        return documentSnapshot.getReference()
                .collection(StoreField.TICKETS_INFOR)
                .orderBy(StoreField.TicketFields.TICKETS_PRICE, Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot ticketDoc = task.getResult().getDocuments().get(0);
                        Long price = ticketDoc.getLong(StoreField.TicketFields.TICKETS_PRICE);
                        if (price != null) {
                            event.setStartingPrice(price);
                        }
                    }
                    return event;
                });
    }

    private void fetchRecentTicket(String userId,
                                   HomeUser user,
                                   List<HomeEvent> events,
                                   HomeDataCallback callback) {

        firestore.collection(StoreField.ORDERS)
                .whereEqualTo(StoreField.OrderFields.USER_ID, userId)
                .limit(1)
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    RecentTicketInfo ticketInfo = mapRecentTicket(orderSnapshot);
                    List<HomeArtist> artists = buildArtists(events);
                    HomeContent content = new HomeContent(user, events, artists, ticketInfo);
                    callback.onSuccess(content);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private RecentTicketInfo mapRecentTicket(QuerySnapshot orderSnapshot) {
        if (orderSnapshot == null || orderSnapshot.isEmpty()) {
            return RecentTicketInfo.empty();
        }

        DocumentSnapshot order = orderSnapshot.getDocuments().get(0);
        List<Map<String, Object>> ticketItems = extractTicketItems(order);

        if (ticketItems.isEmpty()) {
            return RecentTicketInfo.empty();
        }

        Map<String, Object> firstItem = ticketItems.get(0);
        String ticketId = stringValue(firstItem.get("tickets_infor_id"));
        long quantity = longValue(firstItem.get("quantity"));
        long totalPrice = longValue(order.get("total_price"));

        String title = ticketId.isEmpty()
                ? "Vé vừa đặt"
                : String.format(Locale.getDefault(), "%s x%s", ticketId, quantity);
        String subtitle = totalPrice > 0
                ? currencyFormat(totalPrice)
                : "Đang xử lý thanh toán";

        return new RecentTicketInfo(title, subtitle, true);
    }

    @NonNull
    private List<Map<String, Object>> extractTicketItems(DocumentSnapshot order) {
        Object rawList = order.get("tickets_list");
        if (!(rawList instanceof List)) {
            return Collections.emptyList();
        }
        List<?> list = (List<?>) rawList;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                //noinspection unchecked
                result.add(new HashMap<>((Map<String, Object>) item));
            }
        }
        return result;
    }

    private List<HomeArtist> buildArtists(List<HomeEvent> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> artistCounter = new HashMap<>();
        for (HomeEvent event : events) {
            if (event.getCast() == null || event.getCast().isEmpty()) {
                continue;
            }
            String[] artists = event.getCast().split("[,/•]");
            for (String rawName : artists) {
                String name = rawName.trim();
                if (name.isEmpty()) continue;
                artistCounter.put(name, artistCounter.getOrDefault(name, 0) + 1);
            }
        }
        List<HomeArtist> artists = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : artistCounter.entrySet()) {
            artists.add(new HomeArtist(entry.getKey(), entry.getValue()));
        }
        return artists;
    }

    private long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private String currencyFormat(long amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
