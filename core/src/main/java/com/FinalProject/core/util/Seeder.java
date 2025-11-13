package com.FinalProject.core.util;

import android.util.Log;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Seeder {

    private static final String TAG = "Seeder";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ORGANIZER_ID = "seed_user_organizer";
    private static final String ATTENDEE_ID = "seed_user_attendee";
    private static final String EVENT_ID = "seed_event_live_concert";
    private static final String ORDER_ID = "seed_order_recent";

    public static void runSeed(){
        seedUsers();
        seedEvent();
        seedTickets();
        seedOrder();
        seedReview();
    }

    private static void seedUsers() {
        upsertUser(
                ORGANIZER_ID,
                "Tôn Minh Đăng",
                "0901234567",
                "tonminhdang9@gmail.com",
                "organizer"
        );

        upsertUser(
                ATTENDEE_ID,
                "Minh Anh",
                "0934567890",
                "523h0011@student.tdtu.edu.vn",
                "attendee"
        );
    }

    private static void upsertUser(String documentId, String fullName, String phone, String email, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put(StoreField.UserFields.FULLNAME, fullName);
        data.put(StoreField.UserFields.PHONE, phone);
        data.put(StoreField.UserFields.EMAIL, email);
        data.put(StoreField.UserFields.ROLE, role);

        db.collection(StoreField.USER_INFOR)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "Seeded user: " + email))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed user: " + email, e));
    }

    private static void seedEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put(StoreField.EventFields.EVENT_NAME, "Live Concert Sơn Tùng");
        event.put("event_descrip", "Đêm nhạc đặc biệt tại Hà Nội");
        event.put("event_start", "2025-12-10T19:00:00Z");
        event.put("event_end", "2025-12-10T22:00:00Z");
        event.put("cast", "Sơn Tùng M-TP");
        event.put("location", "SVĐ Mỹ Đình");
        event.put("event_type", "Music");
        event.put(StoreField.EventFields.ORGANIZER_UID, ORGANIZER_ID);

        db.collection(StoreField.EVENTS)
                .document(EVENT_ID)
                .set(event, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "Seeded event"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed event", e));
    }

    private static void seedTickets() {
        Map<String, Object> vipTicket = new HashMap<>();
        vipTicket.put(StoreField.TicketFields.TICKETS_CLASS, "VIP");
        vipTicket.put(StoreField.TicketFields.TICKETS_QUANTITY, 200);
        vipTicket.put(StoreField.TicketFields.TICKETS_PRICE, 800000);
        vipTicket.put(StoreField.TicketFields.TICKETS_SOLD, 150);

        Map<String, Object> standardTicket = new HashMap<>();
        standardTicket.put(StoreField.TicketFields.TICKETS_CLASS, "Standard");
        standardTicket.put(StoreField.TicketFields.TICKETS_QUANTITY, 400);
        standardTicket.put(StoreField.TicketFields.TICKETS_PRICE, 350000);
        standardTicket.put(StoreField.TicketFields.TICKETS_SOLD, 240);

        db.collection(StoreField.EVENTS)
                .document(EVENT_ID)
                .collection(StoreField.TICKETS_INFOR)
                .document("VIP")
                .set(vipTicket, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed VIP ticket", e));

        db.collection(StoreField.EVENTS)
                .document(EVENT_ID)
                .collection(StoreField.TICKETS_INFOR)
                .document("Standard")
                .set(standardTicket, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed Standard ticket", e));
    }

    private static void seedOrder() {
        TicketItem ticketItem = new TicketItem("VIP", 2);
        Orders order = new Orders(
                ATTENDEE_ID,
                1600000,
                true,
                Collections.singletonList(ticketItem),
                "https://example.com/qr/seed"
        );

        db.collection(StoreField.ORDERS)
                .document(ORDER_ID)
                .set(order.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "Seeded order"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to seed order", e));
    }

    private static void seedReview() {
        Review_API.addReview(
                "Live Concert Sơn Tùng",
                "523h0011@student.tdtu.edu.vn",
                5,
                "Sự kiện rất vui!"
        );
    }
}
