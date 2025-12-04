package com.FinalProject.core.util;

import android.util.Log;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class Order_API {

    private static final String TAG = "Order_API";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * API cũ: tạo Order dựa vào email user + tên event + ticketClass.
     * Vẫn giữ lại để tương thích với chỗ khác (nếu có).
     */
    public static void addOrder(
            @NonNull String userEmail,
            @NonNull String eventName,
            @NonNull String ticketClass,
            int orderQuantity,
            @NonNull String paymentMethod
    ) {
        AtomicReference<String> userRef = new AtomicReference<>();
        AtomicReference<String> eventRef = new AtomicReference<>();

        // 1) Tìm user theo email
        db.collection(StoreField.USER_INFOR)
                .whereEqualTo(StoreField.UserFields.EMAIL, userEmail)
                .get()
                .continueWithTask(userResult -> {
                    QuerySnapshot userSnap = userResult.getResult();
                    if (userResult.isSuccessful() && userSnap != null && !userSnap.isEmpty()) {
                        userRef.set(userSnap.getDocuments().get(0).getId());

                        // 2) Tìm event theo tên
                        return db.collection(StoreField.EVENTS)
                                .whereEqualTo(StoreField.EventFields.EVENT_NAME, eventName)
                                .get();
                    } else {
                        Log.w(TAG, "User not found: " + userEmail);
                        return Tasks.forResult(null);
                    }
                })
                .continueWithTask(eventResult -> {
                    QuerySnapshot eventSnap = eventResult.getResult();
                    if (eventResult.isSuccessful() && eventSnap != null && !eventSnap.isEmpty()) {
                        String eventId = eventSnap.getDocuments().get(0).getId();
                        eventRef.set(eventId);

                        // 3) Tìm tickets_infor theo tickets_class
                        return db.collection(StoreField.EVENTS)
                                .document(eventId)
                                .collection(StoreField.TICKETS_INFOR)
                                .whereEqualTo(StoreField.TicketFields.TICKETS_CLASS, ticketClass)
                                .get();
                    } else {
                        Log.w(TAG, "Event not found: " + eventName);
                        return Tasks.forResult(null);
                    }
                })
                .continueWithTask(ticketResult -> {
                    QuerySnapshot ticketSnap = ticketResult.getResult();
                    if (ticketResult.isSuccessful() && ticketSnap != null && !ticketSnap.isEmpty()) {
                        // Lấy document vé đầu tiên khớp tickets_class
                        String ticketId = ticketSnap.getDocuments().get(0).getId();

                        // Lấy GIÁ vé từ field tickets_price
                        Long priceLong = ticketSnap.getDocuments()
                                .get(0)
                                .getLong(StoreField.TicketFields.TICKETS_PRICE);
                        int eachPrice = (priceLong != null) ? priceLong.intValue() : 0;

                        int totalPrice = orderQuantity * eachPrice;
                        String userId = userRef.get();
                        String eventId = eventRef.get();

                        if (userId == null || eventId == null) {
                            Log.w(TAG, "UserId or EventId is null, abort addOrder.");
                            return Tasks.forResult(null);
                        }

                        TicketItem ticketItem = new TicketItem(ticketId, orderQuantity);
                        List<TicketItem> ticketItems = new ArrayList<>();
                        ticketItems.add(ticketItem);

                        // payment_status = false (chưa thanh toán) theo logic cũ
                        Orders newOrder = new Orders(userId, totalPrice, false, ticketItems, paymentMethod);
                        return db.collection(StoreField.ORDERS)
                                .add(newOrder.toMap());
                    } else {
                        Log.w(TAG, "Ticket info not found for event: " + eventName);
                        return Tasks.forResult(null);
                    }
                })
                .addOnSuccessListener(docRef -> {
                    if (docRef != null) {
                        Log.d(TAG, "Order added successfully: " + docRef.getId());
                    } else {
                        Log.w(TAG, "Order add skipped (previous step failed).");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding order", e));
    }

    /**
     * === API MỚI – Dành riêng cho feature_booking ===
     *
     * Tạo Order cho 1 event theo schema mới đang dùng trong feature_booking.
     *
     * Firestore:
     *  Collection: Orders (StoreField.ORDERS)
     *    └── doc {orderId}:
     *          - user_id          (StoreField.OrderFields.USER_ID)
     *          - total_price      (StoreField.OrderFields.TOTAL_PRICE, long)
     *          - is_paid          (StoreField.OrderFields.IS_PAID, boolean)
     *          - payment_method   (StoreField.OrderFields.PAYMENT_METHOD, String)
     *          - event_id         (String)
     *          - show_id          (String)
     *          - qr_code          (String, hiện để "")
     *          - ticket_items     (StoreField.OrderFields.TICKET_ITEMS): List<Map<String,Object>>
     *              ├── tickets_infor_id
     *              ├── tickets_class
     *              ├── quantity   (int)
     *              └── price_each (long)
     *          - checked_in       (boolean)
     *          - checked_in_at    (Timestamp/null)
     *          - seats            (List<String>)  // 🔹 danh sách ghế đã giữ
     *
     * @param paymentMethod "CARD" / "WALLET" / "QR" / ...
     * @param seats         danh sách ghế user chọn, ví dụ: ["A3", "A4", "B1"] – có thể null
     */
    @NonNull
    public static Task<String> createOrderForEvent(
            @NonNull String userId,
            @NonNull String eventId,
            @Nullable String showId,
            @NonNull Map<String, Integer> qtyByType,
            @NonNull String paymentMethod,
            @Nullable List<String> seats
    ) {
        Log.d("Order_API", "=== CREATE ORDER API START ===");
        Log.d("Order_API", "UserId: " + userId);
        Log.d("Order_API", "EventId: " + eventId);
        Log.d("Order_API", "ShowId: " + showId);
        Log.d("Order_API", "PaymentMethod: " + paymentMethod);
        Log.d("Order_API", "QtyByType: " + qtyByType);
        Log.d("Order_API", "Seats: " + (seats != null ? seats.size() : 0));
        
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        if (qtyByType.isEmpty()) {
            Log.e("Order_API", "QtyByType is empty!");
            tcs.setException(new IllegalArgumentException("Không có vé nào được chọn."));
            return tcs.getTask();
        }

        // Bước 1: load Tickets_infor của event để map typeId -> docRef, price
        TicketS_Infor_API.getTicketInforByEventId(eventId)
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        tcs.setException(new IllegalStateException(
                                "Không tải được tickets_infor cho eventId=" + eventId));
                        return;
                    }

                    List<Map<String, Object>> ticketList = new ArrayList<>();
                    List<DocumentReference> ticketDocRefs = new ArrayList<>();
                    List<Integer> ticketQtys = new ArrayList<>();
                    long totalPrice = 0L;

                    // Duyệt từng loại vé (STD / VIP / PREMIUM ...) mà user chọn
                    for (Map.Entry<String, Integer> entry : qtyByType.entrySet()) {
                        String typeId = entry.getKey();
                        Integer qtyObj = entry.getValue();
                        int qty = (qtyObj != null) ? qtyObj : 0;
                        if (qty <= 0) continue;

                        // Tìm doc Tickets_infor có tickets_class = typeId
                        DocumentSnapshot matchedDoc = null;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String cls = doc.getString(StoreField.TicketFields.TICKETS_CLASS);
                            if (cls != null && cls.equalsIgnoreCase(typeId)) {
                                matchedDoc = doc;
                                break;
                            }
                        }

                        if (matchedDoc == null) {
                            Log.w(TAG, "Không tìm thấy tickets_infor cho typeId = " + typeId);
                            continue;
                        }

                        String ticketInforId = matchedDoc.getId();
                        String cls = matchedDoc.getString(StoreField.TicketFields.TICKETS_CLASS);
                        Long priceLong = matchedDoc.getLong(StoreField.TicketFields.TICKETS_PRICE);
                        long priceEach = (priceLong != null) ? priceLong : 0L;

                        Map<String, Object> item = new HashMap<>();
                        item.put("tickets_infor_id", ticketInforId);
                        item.put("tickets_class", cls);
                        item.put("quantity", qty);
                        item.put("price_each", priceEach);

                        ticketList.add(item);
                        totalPrice += priceEach * qty;

                        // Lưu ref + qty để lát nữa tăng tickets_sold
                        ticketDocRefs.add(matchedDoc.getReference());
                        ticketQtys.add(qty);
                    }

                    if (ticketList.isEmpty()) {
                        tcs.setException(new IllegalStateException(
                                "Không có vé hợp lệ để tạo đơn hàng."));
                        return;
                    }

                    // Bước 2: build orderData đúng schema mới
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put(StoreField.OrderFields.USER_ID, userId);
                    orderData.put(StoreField.OrderFields.TOTAL_PRICE, totalPrice);
                    orderData.put(StoreField.OrderFields.IS_PAID, true);               // demo: luôn true
                    orderData.put(StoreField.OrderFields.PAYMENT_METHOD, paymentMethod);
                    orderData.put("event_id", eventId);
                    orderData.put("show_id", showId == null ? "" : showId);
                    orderData.put("qr_code", ""); // sẽ cập nhật sau nếu cần
                    orderData.put(StoreField.OrderFields.TICKET_ITEMS, ticketList);
                    // Trạng thái check-in ban đầu
                    orderData.put("checked_in", false);
                    orderData.put("checked_in_at", null);

                    // 🔹 Lưu danh sách ghế đã giữ (nếu có)
                    if (seats != null && !seats.isEmpty()) {
                        orderData.put("seats", new ArrayList<>(seats));
                    }

                    // Bước 3: dùng WriteBatch để:
                    //  - Tạo Order
                    //  - Tăng tickets_sold cho từng Tickets_infor tương ứng
                    WriteBatch batch = db.batch();

                    DocumentReference orderRef =
                            db.collection(StoreField.ORDERS).document(); // tự sinh orderId
                    batch.set(orderRef, orderData);

                    for (int i = 0; i < ticketDocRefs.size(); i++) {
                        DocumentReference ticketRef = ticketDocRefs.get(i);
                        int qty = ticketQtys.get(i);
                        if (qty <= 0) continue;

                        // tickets_sold += qty (atomic trên Firestore)
                        batch.update(
                                ticketRef,
                                StoreField.TicketFields.TICKETS_SOLD,
                                FieldValue.increment(qty)
                        );
                    }

                    Log.d(TAG, "Committing batch with OrderId: " + orderRef.getId());
                    Log.d(TAG, "Order data - userId: " + userId);
                    
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "✅ Order SUCCESSFULLY created: " + orderRef.getId());
                                Log.d(TAG, "✅ UserId in order: " + userId);
                                tcs.setResult(orderRef.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ FAILED to create order", e);
                                tcs.setException(e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi load Tickets_infor cho eventId=" + eventId, e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    /**
     * Convenience overload (KHÔNG có tham số seats) – giữ tương thích code cũ.
     */
    @NonNull
    public static Task<String> createOrderForEvent(
            @NonNull String userId,
            @NonNull String eventId,
            @Nullable String showId,
            @NonNull Map<String, Integer> qtyByType,
            @NonNull String paymentMethod
    ) {
        // Gọi bản đầy đủ với seats = null
        return createOrderForEvent(userId, eventId, showId, qtyByType, paymentMethod, null);
    }

    /**
     * Convenience overload: giữ tương thích code cũ.
     * paymentMethod mặc định = "booking_demo".
     */
    @NonNull
    public static Task<String> createOrderForEvent(
            @NonNull String userId,
            @NonNull String eventId,
            @Nullable String showId,
            @NonNull Map<String, Integer> qtyByType
    ) {
        return createOrderForEvent(userId, eventId, showId, qtyByType, "booking_demo", null);
    }

    /**
     * API mới – dùng model Orders cũ (nếu phía feature đã build sẵn TicketItem, totalPrice, v.v.).
     */
    @NonNull
    public static Task<String> addOrderForBooking(
            @NonNull String userId,
            int totalPrice,
            boolean paymentStatus,
            @NonNull List<TicketItem> ticketItems,
            @NonNull String paymentMethod,
            @Nullable String qrCode   // có thể null nếu chưa cần lưu
    ) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        Orders newOrder = new Orders(userId, totalPrice, paymentStatus, ticketItems, paymentMethod);
        // Nếu Orders có setQrCode thì có thể set thêm:
        // if (qrCode != null) newOrder.setQrCode(qrCode);

        db.collection(StoreField.ORDERS)
                .add(newOrder.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Order added (booking) successfully: " + docRef.getId());
                    tcs.setResult(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding booking order", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    /**
     * Overload tiện dùng: paymentStatus mặc định = true.
     */
    @NonNull
    public static Task<String> addOrderForBooking(
            @NonNull String userId,
            int totalPrice,
            @NonNull List<TicketItem> ticketItems,
            @NonNull String paymentMethod
    ) {
        return addOrderForBooking(userId, totalPrice, true, ticketItems, paymentMethod, null);
    }

    /**
     * Lấy danh sách đơn hàng theo userId (dùng cho MyTickets).
     */
    @NonNull
    public static Task<QuerySnapshot> getOrdersByUserId(@NonNull String userId) {
        return db.collection(StoreField.ORDERS)
                .whereEqualTo(StoreField.OrderFields.USER_ID, userId)
                .get();
    }

    /**
     * Lấy Orders theo userId với Source (cache/server/default)
     * Dùng Source.SERVER để force reload từ server sau khi tạo order mới
     */
    @NonNull
    public static Task<QuerySnapshot> getOrdersByUserId(@NonNull String userId, @NonNull Source source) {
        return db.collection(StoreField.ORDERS)
                .whereEqualTo(StoreField.OrderFields.USER_ID, userId)
                .get(source);
    }

    /**
     * Lấy 1 Order theo ID (dùng cho ScanTicket, TicketDetail nếu cần).
     */
    @NonNull
    public static Task<DocumentSnapshot> getOrderById(@NonNull String orderId) {
        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .get();
    }

    /**
     * Cập nhật trường qr_code cho 1 đơn hàng.
     * Dùng khi đã có orderId và đã build được payload QR (JSON/string).
     */
    @NonNull
    public static Task<Void> updateQrCode(@NonNull String orderId,
                                          @NonNull String qrPayload) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("qr_code", qrPayload);

        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .update(updates);
    }

    /**
     * Đánh dấu Order đã check-in: checked_in = true, checked_in_at = serverTimestamp().
     * Dùng cho luồng ScanTicketFragment.
     */
    @NonNull
    public static Task<Void> markOrderCheckedIn(@NonNull String orderId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("checked_in", true);
        updates.put("checked_in_at", FieldValue.serverTimestamp());

        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .update(updates);
    }

    /**
     * Cập nhật transaction_id và payment_timestamp sau khi payment thành công.
     * Gọi từ CheckoutFragment sau khi PaymentOrchestrator.onSuccess().
     */
    @NonNull
    public static Task<Void> updatePaymentTransaction(@NonNull String orderId,
                                                      @NonNull String transactionId,
                                                      long paymentTimestamp) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("transaction_id", transactionId);
        updates.put("payment_timestamp", paymentTimestamp);
        updates.put("is_paid", true); // Đánh dấu đã thanh toán

        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .update(updates);
    }

    /**
     * Cập nhật promotion info vào Order sau khi apply promotion.
     * Gọi từ CheckoutFragment khi user apply promo code hợp lệ.
     */
    @NonNull
    public static Task<Void> updatePromotionInfo(@NonNull String orderId,
                                                 @NonNull String promotionId,
                                                 @NonNull String promotionCode,
                                                 int discountAmount,
                                                 int originalPrice) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("promotion_id", promotionId);
        updates.put("promotion_code", promotionCode);
        updates.put("discount_amount", discountAmount);
        updates.put("original_price", originalPrice);

        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .update(updates);
    }

    // ============================================================================================
    //  NEW: Seats helper cho SeatSelectionFragment / BookingRepository
    // ============================================================================================

    /**
     * Lấy danh sách ghế đã RESERVED cho 1 event.
     *
     * Hiện tại triển khai đơn giản:
     *  - Đọc collection Orders
     *  - where event_id == eventId
     *  - where is_paid == true
     *  - gom tất cả phần tử trong field "seats" (List<String>) vào Set<String>
     *
     * => SeatSelectionFragment chỉ cần gọi BookingRepository.getReservedSeatsForEvent(eventId)
     *    là tránh cho user chọn trùng ghế.
     */
    @NonNull
    public static Task<Set<String>> getReservedSeatsForEvent(@NonNull String eventId) {
        TaskCompletionSource<Set<String>> tcs = new TaskCompletionSource<>();

        db.collection(StoreField.ORDERS)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo(StoreField.OrderFields.IS_PAID, true)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> seatSet = new HashSet<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            @SuppressWarnings("unchecked")
                            List<String> seats = (List<String>) doc.get("seats");
                            if (seats == null) continue;
                            for (String s : seats) {
                                if (s == null) continue;
                                String trimmed = s.trim();
                                if (!trimmed.isEmpty()) {
                                    seatSet.add(trimmed);
                                }
                            }
                        }
                    }
                    tcs.setResult(seatSet);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getReservedSeatsForEvent for eventId=" + eventId, e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    /**
     * Đánh dấu seats là RESERVED cho event/show.
     *
     * Hiện tại reserved seats đã được derive trực tiếp từ field "seats" trong Orders,
     * nên hàm này chỉ log + trả về Task thành công để giữ tương thích.
     *
     * Sau này nếu bạn muốn tách riêng subcollection
     *  Events/{eventId}/seats/{seatId}
     * thì chỉ cần implement lại phần thân hàm này.
     */
    @NonNull
    public static Task<Void> markSeatsReserved(
            @NonNull String eventId,
            @Nullable String showId,
            @NonNull List<String> seats,
            @NonNull String orderId
    ) {
        Log.d(TAG, "markSeatsReserved() called for eventId=" + eventId
                + ", showId=" + showId + ", orderId=" + orderId + ", seats=" + seats);
        return Tasks.forResult(null);
    }
    public static Task<QuerySnapshot> getOrdersByEventId(String eventId){
        return db.collection(StoreField.ORDERS)
                .whereEqualTo(StoreField.OrderFields.EVENT_ID, eventId)
                .get();
    }

    public static Task<String> getUserIdByOrderId(String orderId) {
        return db.collection(StoreField.ORDERS)
                .document(orderId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            return doc.getString("user_id"); // trả về user_id
                        }
                    }
                    return null; // nếu không tồn tại hoặc lỗi
                });
    }


}
