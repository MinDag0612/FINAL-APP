package com.FinalProject.feature_attendee_manager_organizer.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public Task<Void> sendBroadcast(String eventId, String eventName, String title, String content, List<AttendeeRepository.AttendeeItem> attendees) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        if (attendees == null || attendees.isEmpty()) {
            tcs.setException(new IllegalArgumentException("Không có người nhận"));
            return tcs.getTask();
        }
        saveNotifications(attendees, eventId, eventName, title, content)
                .addOnSuccessListener(unused -> sendFcm(attendees, eventId, eventName, title, content, tcs))
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    private Task<Void> saveNotifications(List<AttendeeRepository.AttendeeItem> attendees, String eventId, String eventName, String title, String content) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        WriteBatch batch = db.batch();
        for (AttendeeRepository.AttendeeItem a : attendees) {
            if (a.userId == null) continue;
            String docId = UUID.randomUUID().toString();
            batch.set(
                    db.collection("Notifications").document(docId),
                    new NotificationPayload(a.userId, eventId, eventName, title, content).toMap()
            );
        }
        batch.commit()
                .addOnSuccessListener(unused -> tcs.setResult(null))
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    private void sendFcm(List<AttendeeRepository.AttendeeItem> attendees, String eventId, String eventName, String title, String content, TaskCompletionSource<Void> tcs) {
        List<String> tokens = new ArrayList<>();
        List<String> uids = new ArrayList<>();
        for (AttendeeRepository.AttendeeItem a : attendees) {
            if (a.userId != null) uids.add(a.userId);
        }
        if (uids.isEmpty()) {
            tcs.setResult(null);
            return;
        }

        db.collection("User_Infor")
                .whereIn("uid", uids) // assumes document contains field uid; if docId == uid we still read token
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String token = doc.getString("fcm_token");
                            if (token != null && !token.isEmpty()) tokens.add(token);
                        }
                    }
                    sendFcmRequest(tokens, eventId, eventName, title, content, tcs);
                })
                .addOnFailureListener(tcs::setException);
    }

    private void sendFcmRequest(List<String> tokens, String eventId, String eventName, String title, String content, TaskCompletionSource<Void> tcs) {
        if (tokens == null || tokens.isEmpty()) {
            tcs.setException(new IllegalStateException("Không tìm thấy token FCM nào"));
            return;
        }
        String serverKey = com.FinalProject.feature_attendee_manager_organizer.BuildConfig.FCM_SERVER_KEY;
        if (serverKey == null || serverKey.isEmpty()) {
            tcs.setException(new IllegalStateException("Chưa cấu hình FCM_SERVER_KEY"));
            return;
        }

        try {
            StringBuilder sbTokens = new StringBuilder();
            sbTokens.append("[");
            for (int i = 0; i < tokens.size(); i++) {
                if (i > 0) sbTokens.append(",");
                sbTokens.append("\"").append(tokens.get(i)).append("\"");
            }
            sbTokens.append("]");

            String json = "{"
                    + "\"registration_ids\":" + sbTokens.toString() + ","
                    + "\"notification\":{"
                    + "\"title\":\"" + escape(title) + "\","
                    + "\"body\":\"" + escape(content) + "\""
                    + "},"
                    + "\"data\":{"
                    + "\"event_id\":\"" + escape(eventId) + "\","
                    + "\"event_name\":\"" + escape(eventName) + "\""
                    + "}"
                    + "}";

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=" + serverKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            new Thread(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        tcs.setResult(null);
                    } else {
                        tcs.setException(new IOException("FCM error: " + response.code() + " " + response.message()));
                    }
                } catch (Exception e) {
                    tcs.setException(e);
                }
            }).start();
        } catch (Exception e) {
            tcs.setException(e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    private static class NotificationPayload {
        final String uid;
        final String eventId;
        final String eventName;
        final String title;
        final String content;

        NotificationPayload(String uid, String eventId, String eventName, String title, String content) {
            this.uid = uid;
            this.eventId = eventId;
            this.eventName = eventName;
            this.title = title;
            this.content = content;
        }

        java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("uid", uid);
            map.put("event_id", eventId);
            map.put("event_name", eventName);
            map.put("title", title);
            map.put("content", content);
            map.put("created_at", Timestamp.now());
            return map;
        }
    }
}
