package com.FinalProject.feature_home.data;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_home.model.HomeArtist;
import com.FinalProject.feature_home.model.HomeContent;
import com.FinalProject.feature_home.model.HomeEvent;
import com.FinalProject.feature_home.model.HomeUser;
import com.FinalProject.feature_home.model.RecentTicketInfo;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeRepository {

    private final HomeApi api;
    private final EventRepository eventRepo;
    private final NumberFormat currencyFormat;

    public interface HomeDataCallback {
        void onSuccess(HomeContent content);
        void onError(String message);
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

    public HomeRepository() {
        this(new FirebaseHomeApi(), null);
    }

    public HomeRepository(HomeApi api, EventRepository eventRepo) {
        this.api = api;
        this.eventRepo = eventRepo != null ? eventRepo : new EventRepository(api);
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void loadHomeContent(HomeDataCallback callback) {
        FirebaseUser currentUser = api.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        String email = currentUser.getEmail();
        api.getUserInfo(email)
                .addOnSuccessListener(userSnapshot -> handleUserSnapshot(userSnapshot, email, callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void handleUserSnapshot(DocumentSnapshot userSnapshot, String email, HomeDataCallback callback) {
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

        api.getLatestEvents(10)
                .addOnSuccessListener(eventsSnapshot ->
                        eventRepo.buildEvents(eventsSnapshot)
                                .addOnSuccessListener(events ->
                                        fetchRecentTicket(userId, homeUser, events, callback))
                                .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void fetchRecentTicket(String userId, HomeUser user, List<HomeEvent> events, HomeDataCallback callback) {
        api.getOrdersByUserId(userId)
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
                ? currencyFormat.format(totalPrice)
                : "Đang xử lý thanh toán";

        return new RecentTicketInfo(title, subtitle, true);
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
}
