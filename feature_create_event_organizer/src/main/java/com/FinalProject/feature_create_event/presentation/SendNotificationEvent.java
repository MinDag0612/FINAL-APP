package com.FinalProject.feature_create_event.presentation;

import android.content.Context;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.util.HandleNotification;

import java.util.List;

public class SendNotificationEvent {
    public static void sendUpdateNoti(Context context, List<String> listUser, Events newEvent){
        String title = "Sự kiện " + newEvent.getEvent_name() + " đã được cập nhật";
        String body = "Sự kiện " + newEvent.getEvent_name() + " đã được cập nhật\n" + newEvent.toMap().toString();
        for (String userToken : listUser) {
            new Thread(() -> {
                HandleNotification.sendNotification(context, userToken, title, body);
            }).start();
        }
    }
}
