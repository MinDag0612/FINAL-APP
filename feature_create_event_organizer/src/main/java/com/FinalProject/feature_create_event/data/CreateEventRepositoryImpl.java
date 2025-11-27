package com.FinalProject.feature_create_event.data;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.core.util.UserInfor_API;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CreateEventRepositoryImpl {
    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface GetFCMCallback {
        void onSuccess(List<String> fcmTokens);
        void onFailure(String message);
    }

    public void createEventWithTicket(Events event, TicketInfor ticketInfor, Callback callback) {
        Event_API.addEventWithTicket(event, ticketInfor)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static Task<List<String>> getFcmByEventId(String eventId) {
        ArrayList<Task<String>> fcmTasks = new ArrayList<>();

        Task<QuerySnapshot> ordersTask = Order_API.getOrdersByEventId(eventId);

        return ordersTask.continueWithTask(orderTask -> {
            if (!orderTask.isSuccessful() || orderTask.getResult() == null) {
                return Tasks.forResult(new ArrayList<>());
            }

            for (DocumentSnapshot orderDoc : orderTask.getResult().getDocuments()) {
                String orderId = orderDoc.getId();

                // Task lấy user_id
                Task<String> userTask = Order_API.getUserIdByOrderId(orderId)
                        .continueWithTask(userIdTask -> {
                            String userId = userIdTask.getResult();
                            if (userId == null) return Tasks.forResult(null);

                            // Task lấy fcm token
                            return UserInfor_API.getFcmTokenByUserId(userId)
                                    .continueWith(fcmTask -> {
                                        if (fcmTask.isSuccessful() && fcmTask.getResult() != null) {
                                            return fcmTask.getResult().getString("fcm_token");
                                        }
                                        return null;
                                    });
                        });

                fcmTasks.add(userTask);
            }

            // Khi tất cả task hoàn thành
            return Tasks.whenAllSuccess(fcmTasks);
        }).continueWith(task -> {
            // Lọc bỏ null
            List<String> result = new ArrayList<>();
            for (Object obj : task.getResult()) {
                if (obj != null) result.add((String) obj);
            }
            return result;
        });
    }
}
