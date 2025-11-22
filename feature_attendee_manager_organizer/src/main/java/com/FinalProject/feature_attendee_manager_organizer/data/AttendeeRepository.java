package com.FinalProject.feature_attendee_manager_organizer.data;

import android.text.TextUtils;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.Event_API;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttendeeRepository {

    public static class AttendeeItem {
        public final String userId;
        public final String name;
        public final String email;
        public final int totalTickets;
        public final int totalPrice;

        public AttendeeItem(String userId, String name, String email, int totalTickets, int totalPrice) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.totalTickets = totalTickets;
            this.totalPrice = totalPrice;
        }
    }

    public interface Callback {
        void onSuccess(List<AttendeeItem> attendees);
        void onFailure(String message);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadAttendees(String eventId, Callback callback) {
        if (TextUtils.isEmpty(eventId)) {
            callback.onFailure("Thiếu eventId");
            return;
        }
        Task<QuerySnapshot> ticketTask = Event_API.getTicketsForEvent(eventId, -1);
        Task<QuerySnapshot> ordersTask = db.collection(StoreField.ORDERS).get();

        Tasks.whenAllSuccess(ticketTask, ordersTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot ticketSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot ordersSnap = (QuerySnapshot) results.get(1);

                    Set<String> ticketIds = new HashSet<>();
                    if (ticketSnap != null) {
                        for (QueryDocumentSnapshot doc : ticketSnap) {
                            ticketIds.add(doc.getId());
                        }
                    }

                    if (ticketIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    Map<String, Aggregated> byUser = new HashMap<>();
                    if (ordersSnap != null) {
                        for (QueryDocumentSnapshot orderDoc : ordersSnap) {
                            String userId = orderDoc.getString(StoreField.OrderFields.USER_ID);
                            List<Map<String, Object>> ticketItems = (List<Map<String, Object>>) orderDoc.get("tickets_list");
                            if (ticketItems == null) continue;
                            int totalPrice = orderDoc.getLong("total_price") != null ? orderDoc.getLong("total_price").intValue() : 0;
                            for (Map<String, Object> item : ticketItems) {
                                String ticketId = item.get("tickets_infor_id") != null ? item.get("tickets_infor_id").toString() : null;
                                Number qtyNum = item.get("quantity") instanceof Number ? (Number) item.get("quantity") : null;
                                int qty = qtyNum != null ? qtyNum.intValue() : 0;
                                if (ticketIds.contains(ticketId) && userId != null) {
                                    Aggregated agg = byUser.getOrDefault(userId, new Aggregated(userId));
                                    agg.totalTickets += qty;
                                    agg.totalPrice += totalPrice;
                                    byUser.put(userId, agg);
                                }
                            }
                        }
                    }

                    fetchUsers(byUser.values())
                            .addOnSuccessListener(userMap -> {
                                List<AttendeeItem> list = new ArrayList<>();
                                for (Aggregated agg : byUser.values()) {
                                    String name = userMap.containsKey(agg.userId) ? userMap.get(agg.userId).name : agg.userId;
                                    String email = userMap.containsKey(agg.userId) ? userMap.get(agg.userId).email : "";
                                    list.add(new AttendeeItem(agg.userId, name, email, agg.totalTickets, agg.totalPrice));
                                }
                                callback.onSuccess(list);
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private Task<Map<String, UserInfo>> fetchUsers(Iterable<Aggregated> users) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (Aggregated agg : users) {
            tasks.add(db.collection(StoreField.USER_INFOR).document(agg.userId).get());
        }
        TaskCompletionSource<Map<String, UserInfo>> tcs = new TaskCompletionSource<>();
        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(result -> {
                    Map<String, UserInfo> map = new HashMap<>();
                    for (Object obj : result) {
                        if (obj instanceof DocumentSnapshot) {
                            DocumentSnapshot snap = (DocumentSnapshot) obj;
                            map.put(snap.getId(), new UserInfo(
                                    snap.getString(StoreField.UserFields.FULLNAME),
                                    snap.getString(StoreField.UserFields.EMAIL)
                            ));
                        }
                    }
                    tcs.setResult(map);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    private static class Aggregated {
        final String userId;
        int totalTickets = 0;
        int totalPrice = 0;

        Aggregated(String userId) {
            this.userId = userId;
        }
    }

    private static class UserInfo {
        final String name;
        final String email;

        UserInfo(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
