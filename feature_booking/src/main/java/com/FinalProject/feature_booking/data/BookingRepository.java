package com.FinalProject.feature_booking.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.core.util.TicketS_Infor_API;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BookingRepository – lớp “bridge” giữa feature_booking và tầng core API.
 *
 * Thay vì gọi FirebaseFirestore trực tiếp, hầu hết logic đã được
 * đẩy xuống các API trong module core:
 *
 *  - Event_API.getEventById(eventId)
 *  - TicketS_Infor_API.getTicketInforByEventId(eventId)
 *  - Order_API.createOrderForEvent(userId, eventId, showId, qtyByType, paymentMethod)
 *  - Order_API.createOrderForEvent(userId, eventId, showId, qtyByType, paymentMethod, seats)
 *  - Order_API.getOrdersByUserId(userId)
 *  - Order_API.getOrderById(orderId)
 *  - Order_API.updateQrCode(orderId, qrPayload)
 *  - Order_API.getReservedSeatsForEvent(eventId)
 *  - Order_API.markSeatsReserved(eventId, showId, seats, orderId)
 *
 * DB_Structure liên quan:
 *
 * Events (collection)
 *  └── {event_id}
 *       └── Tickets_infor (subcollection)
 *            └── {tickets_infor_id}
 *                 ├── tickets_class    (STD / VIP / VVIP / PREMIUM / GENERAL ...)
 *                 ├── tickets_price
 *                 ├── tickets_quantity
 *                 └── tickets_sold
 *
 * Orders (collection)
 *  └── {orderId}
 *       ├── user_id
 *       ├── total_price
 *       ├── is_paid              (boolean)
 *       ├── payment_method       ("CARD"/"WALLET"/"QR"/...)
 *       ├── event_id
 *       ├── show_id
 *       ├── qr_code              (JSON payload dùng cho QR scan)
 *       ├── seats                (List<String> – ghế đã mua, nếu có)
 *       └── ticket_items: [ { tickets_infor_id, tickets_class, quantity, price_each }, ... ]
 *
 * Seats (có thể được triển khai bên trong Order_API nếu muốn tách riêng):
 *  Events/{event_id}/seats/{seatId}
 *      ├── status     : "RESERVED" / "AVAILABLE" / ...
 *      ├── order_id   : {orderId}
 *      └── show_id    : {showId} (nếu bạn tách nhiều suất diễn)
 */
public class BookingRepository {

    private static volatile BookingRepository INSTANCE;

    private BookingRepository() {
        // Không giữ FirebaseFirestore ở đây nữa,
        // mọi thao tác Firestore chính được đẩy xuống core API.
    }

    public static BookingRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (BookingRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BookingRepository();
                }
            }
        }
        return INSTANCE;
    }

    // --------------------------------------------------------------------------------------------
    //  EVENTS
    // --------------------------------------------------------------------------------------------

    /**
     * Lấy DocumentSnapshot của 1 event thông qua Event_API (core layer).
     */
    public Task<DocumentSnapshot> getEventDocument(@NonNull String eventId) {
        return Event_API.getEventById(eventId);
    }

    // --------------------------------------------------------------------------------------------
    //  tickets_infor RAW
    // --------------------------------------------------------------------------------------------

    /**
     * Lấy toàn bộ Tickets_infor của 1 event thông qua TicketS_Infor_API (core layer).
     */
    public Task<QuerySnapshot> getTicketInfos(@NonNull String eventId) {
        return TicketS_Infor_API.getTicketInforByEventId(eventId);
    }

    // --------------------------------------------------------------------------------------------
    //  TicketInfor cho UI (BookingActivity / EventDetail / SeatSelection)
    // --------------------------------------------------------------------------------------------

    /**
     * Map subcollection Tickets_infor -> List<TicketType> cho UI.
     *
     * typeId       = tickets_class (STD/VIP/VVIP/PREMIUM/GENERAL/...)
     * displayName  = cùng giá trị với tickets_class (có thể tuỳ biến nếu muốn hiển thị đẹp hơn)
     *
     * Lưu ý:
     *  - SeatSelectionFragment dùng typeId để suy ra zone (GENERAL/VIP/PREMIUM)
     *    dựa trên tên như "STD", "GENERAL", "VIP", "VVIP"/"PREMIUM"... (tuỳ logic bạn build).
     */
    public Task<List<TicketInfor>> getTicketTypesForEvent(@NonNull String eventId) {
        // Dùng core API để lấy subcollection Tickets_infor cho event
        return TicketS_Infor_API.getTicketInforByEventId(eventId)
                .continueWith(task -> {
                    List<TicketInfor> result = new ArrayList<>();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return result;
                    }

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        TicketInfor info = doc.toObject(TicketInfor.class);
                        if (info != null) {
                            result.add(info);
                        }
                    }
                    return result;
                });
    }


    // --------------------------------------------------------------------------------------------
    //  Orders – tạo đơn vé
    // --------------------------------------------------------------------------------------------

    /**
     * Tạo Order trong collection Orders theo StoreField thông qua Order_API (core layer).
     *
     * KHÔNG truyền seats: dùng cho luồng cũ hoặc sự kiện không phân ghế.
     *
     * @param userId        uid của user (FirebaseAuthHelper.getCurrentUserUid())
     * @param eventId       id sự kiện (Events/{eventId})
     * @param showId        id suất diễn (có thể "" nếu chỉ có 1 suất)
     * @param qtyByType     key = tickets_class (STD/VIP/VVIP/...), value = quantity
     * @param paymentMethod "CARD" / "WALLET" / "QR" / ...
     *
     * @return Task<String> – trả về orderId vừa tạo.
     */
    public Task<String> createOrder(@NonNull String userId,
                                    @NonNull String eventId,
                                    @Nullable String showId,
                                    @NonNull Map<String, Integer> qtyByType,
                                    @NonNull String paymentMethod) {

        return Order_API.createOrderForEvent(userId, eventId, showId, qtyByType, paymentMethod);
    }

    /**
     * Overload giữ tương thích code cũ – nếu không truyền paymentMethod
     * thì mặc định "booking_demo".
     */
    public Task<String> createOrder(@NonNull String userId,
                                    @NonNull String eventId,
                                    @Nullable String showId,
                                    @NonNull Map<String, Integer> qtyByType) {

        return createOrder(userId, eventId, showId, qtyByType, "booking_demo");
    }

    /**
     * 🔹 API mới: tạo Order & lưu luôn danh sách ghế (seats) ngay lúc user confirm checkout.
     *
     * Dùng cho luồng:
     *  - SeatSelectionFragment → CheckoutFragment (có selectedSeats)
     *  - CheckoutFragment gọi createOrder(..., seats)
     *
     * Lúc này Order_API.createOrderForEvent(...) sẽ:
     *  - Tạo Order với ticket_items, total_price, ...
     *  - Ghi thêm field "seats": List<String> ghế đã mua
     *  → SeatSelectionFragment có thể load reserved seats từ Orders để disable ghế.
     */
    public Task<String> createOrder(@NonNull String userId,
                                    @NonNull String eventId,
                                    @Nullable String showId,
                                    @NonNull Map<String, Integer> qtyByType,
                                    @NonNull String paymentMethod,
                                    @NonNull List<String> seats) {

        return Order_API.createOrderForEvent(
                userId,
                eventId,
                showId,
                qtyByType,
                paymentMethod,
                seats
        );
    }

    /**
     * Overload tiện dùng: không truyền paymentMethod, mặc định "booking_demo"
     * nhưng vẫn lưu seats.
     */
    public Task<String> createOrder(@NonNull String userId,
                                    @NonNull String eventId,
                                    @Nullable String showId,
                                    @NonNull Map<String, Integer> qtyByType,
                                    @NonNull List<String> seats) {

        return createOrder(userId, eventId, showId, qtyByType, "booking_demo", seats);
    }

    // --------------------------------------------------------------------------------------------
    //  Orders – lấy danh sách đơn của 1 user
    // --------------------------------------------------------------------------------------------

    /**
     * Dùng Order_API (core layer) để lấy danh sách Order theo userId.
     */
    public Task<QuerySnapshot> getOrdersForUser(@NonNull String userId) {
        return Order_API.getOrdersByUserId(userId);
    }

    /**
     * Lấy Orders của user với Source (để force reload từ server)
     */
    public Task<QuerySnapshot> getOrdersForUser(@NonNull String userId, @NonNull com.google.firebase.firestore.Source source) {
        return Order_API.getOrdersByUserId(userId, source);
    }

    // --------------------------------------------------------------------------------------------
    //  Orders – lấy 1 Order theo ID (TicketDetailFragment, ScanTicketFragment)
    // --------------------------------------------------------------------------------------------

    /**
     * Lấy Order theo ID thông qua Order_API (core layer).
     */
    public Task<DocumentSnapshot> getOrderById(@NonNull String orderId) {
        return Order_API.getOrderById(orderId);
    }

    // --------------------------------------------------------------------------------------------
    //  Orders – cập nhật QR code cho Order
    // --------------------------------------------------------------------------------------------

    /**
     * Cập nhật field qr_code cho 1 Order.
     * qrPayload thường là JSON string chứa ticketId, event, summary, show, ...
     *
     * Được dùng trong CheckoutFragment:
     *  - Sau khi createOrder thành công và có orderId
     *  - Build payload JSON đồng bộ với ScanTicketFragment
     *  - Gọi updateOrderQrCode(orderId, payload)
     */
    public Task<Void> updateOrderQrCode(@NonNull String orderId,
                                        @NonNull String qrPayload) {
        return Order_API.updateQrCode(orderId, qrPayload);
    }

    /**
     * Cập nhật transaction_id và payment_timestamp sau payment thành công.
     */
    public Task<Void> updatePaymentTransaction(@NonNull String orderId,
                                               @NonNull String transactionId,
                                               long paymentTimestamp) {
        return Order_API.updatePaymentTransaction(orderId, transactionId, paymentTimestamp);
    }

    /**
     * Cập nhật promotion info vào Order.
     */
    public Task<Void> updatePromotionInfo(@NonNull String orderId,
                                          @NonNull String promotionId,
                                          @NonNull String promotionCode,
                                          int discountAmount,
                                          int originalPrice) {
        return Order_API.updatePromotionInfo(orderId, promotionId, promotionCode, 
                discountAmount, originalPrice);
    }

    // --------------------------------------------------------------------------------------------
    //  Seats – ghế đã reserved (dùng cho SeatSelectionFragment)
    // --------------------------------------------------------------------------------------------

    /**
     * Lấy danh sách ghế đã RESERVED cho 1 event.
     *
     * Giao tiếp với core thông qua Order_API.getReservedSeatsForEvent(eventId).
     *
     * Một cách triển khai hợp lý ở core:
     *  - Đọc từ collection Orders, filter event_id = eventId, is_paid = true
     *  - Gom tất cả field "seats" (List<String>) lại thành 1 Set<String>
     *  - Trả về Set ghế đã được mua (A1, A2, B3, ...)
     *
     * SeatSelectionFragment sẽ:
     *  - Gọi getReservedSeatsForEvent(eventId)
     *  - Ghế nào có trong Set này sẽ render trạng thái SeatState.RESERVED.
     */
    public Task<Set<String>> getReservedSeatsForEvent(@NonNull String eventId) {
        return Order_API.getReservedSeatsForEvent(eventId);
    }

    // --------------------------------------------------------------------------------------------
    //  Seats – mark ghế đã reserved sau khi tạo order (optional nhưng nên dùng nếu tách riêng)
    // --------------------------------------------------------------------------------------------

    /**
     * Đánh dấu 1 list ghế là RESERVED cho event / show sau khi Order đã được tạo & thanh toán.
     *
     * Giao tiếp với core thông qua Order_API.markSeatsReserved(eventId, showId, seats, orderId).
     *
     * Hai option sử dụng:
     *  1) Nếu đã dùng createOrder(..., seats) và core chỉ đọc trực tiếp từ Orders.seats
     *     → Có thể không cần gọi hàm này nữa.
     *
     *  2) Nếu core muốn lưu thêm subcollection Events/{eventId}/seats
     *     → CheckoutFragment sau khi tạo đơn có thể gọi markSeatsReserved(...) để sync thêm.
     */
    public Task<Void> markSeatsReserved(@NonNull String eventId,
                                        @Nullable String showId,
                                        @NonNull List<String> seats,
                                        @NonNull String orderId) {
        return Order_API.markSeatsReserved(eventId, showId, seats, orderId);
    }
}
