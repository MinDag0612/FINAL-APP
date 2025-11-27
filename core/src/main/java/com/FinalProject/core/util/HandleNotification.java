package com.FinalProject.core.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class HandleNotification {

    private static final String TAG = "HandleNotification";
    private static final String FCM_URL_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";
    private static final String PROJECT_ID = "finalapp-6a396"; // Thay bằng Firebase project ID

    public HandleNotification() {}

    // --- Lấy FCM token của thiết bị ---
    public static void getFcmToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        callback.onTokenReceived(null);
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    callback.onTokenReceived(token);
                });
    }

    public interface TokenCallback {
        void onTokenReceived(String token);
    }

    // --- Lấy Access Token từ Service Account JSON trong assets ---
    public static String getAccessToken(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("serviceAccount.json"); // file trong assets

        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging");
        googleCredentials.refreshIfExpired();
        AccessToken token = googleCredentials.getAccessToken();
        return token.getTokenValue();
    }

    // --- Gửi notification bằng HTTP v1 API ---
    public static void sendNotification(Context context, String fcmToken, String title, String body) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        try {
            String accessToken = getAccessToken(context); // truyền Context từ Activity

            JsonObject message = new JsonObject();
            JsonObject notification = new JsonObject();
            notification.addProperty("title", title);
            notification.addProperty("body", body);

            JsonObject messageContent = new JsonObject();
            messageContent.add("notification", notification);
            messageContent.addProperty("token", fcmToken);

            message.add("message", messageContent);

            RequestBody requestBody = RequestBody.create(
                    message.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            String url = String.format(FCM_URL_TEMPLATE, PROJECT_ID);

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "FCM send failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "FCM Response Send Success: " + response.body().string());
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error getting access token", e);
        }
    }

    // --- Test: lấy token và gửi notification ---
    public static void test(Context context) {
        getFcmToken(token -> {
            if (token != null) {
                // chạy sendNotification trên thread khác
                new Thread(() -> sendNotification(context, token, "Xin chào", "Thông báo test")).start();
            }
        });
    }


}
