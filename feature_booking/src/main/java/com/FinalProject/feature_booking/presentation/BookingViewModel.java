package com.FinalProject.feature_booking.presentation;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.FinalProject.feature_booking.model.OrderResult;
import com.FinalProject.feature_booking.model.Showtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ViewModel cho BookingActivity.
 *
 * - dates / showtimes: vẫn demo (hard-code) vì DB_Structure chưa có showtimes.
 * - ticketTypes: lấy thật từ Firestore: Events/{eventId}/Tickets_infor (model TicketInfor trong core).
 * - book(): tạo document trong Orders (thông qua BookingRepository -> Order_API).
 */
public class BookingViewModel extends ViewModel {

    private final BookingRepository repo;

    public final MutableLiveData<List<String>> dates =
            new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Showtime>> showtimes =
            new MutableLiveData<>(new ArrayList<>());

    // 🔹 ĐÃ ĐỔI: dùng TicketInfor (core) thay vì TicketType (feature_booking)
    public final MutableLiveData<List<TicketInfor>> ticketTypes =
            new MutableLiveData<>(new ArrayList<>());

    public final MutableLiveData<Long> totalPrice =
            new MutableLiveData<>(0L);
    public final MutableLiveData<String> error =
            new MutableLiveData<>(null);

    // Kết quả đặt vé (orderId) – Activity chỉ cần observe
    public final MutableLiveData<OrderResult> orderResult =
            new MutableLiveData<>(null);

    // typeId = tickets_class (STD/VIP/PREMIUM/GENERAL/...)
    // key phải TRÙNG với field tickets_class trong Firestore
    private final Map<String, Integer> qtyByType = new HashMap<>();

    public BookingViewModel(@NonNull BookingRepository repo) {
        this.repo = repo;
    }

    // ====== NGÀY (demo) ======
    public void loadDates(String eventId) {
        // Sau này nếu event có nhiều ngày trong Firestore, map từ repo tại đây.
        dates.setValue(Arrays.asList("2025-12-20", "2025-12-21"));
    }

    // ====== SUẤT DIỄN (demo) ======
    public void loadShowtimes(String eventId, String date) {
        List<Showtime> list = new ArrayList<>();
        list.add(new Showtime("S1", date, "17:30", 0L));
        list.add(new Showtime("S2", date, "19:30", 0L));
        list.add(new Showtime("S3", date, "21:00", 0L));

        showtimes.setValue(list);

        // reset state vé
        qtyByType.clear();
        ticketTypes.setValue(new ArrayList<>());
        totalPrice.setValue(0L);
        orderResult.setValue(null);
    }

    // ====== LOẠI VÉ: lấy thật từ Firestore (TicketInfor trong core) ======
    public void loadTicketTypes(String eventId, String showId) {
        qtyByType.clear();
        totalPrice.setValue(0L);
        orderResult.setValue(null);

        repo.getTicketTypesForEvent(eventId)           // Task<List<TicketInfor>>
                .addOnSuccessListener(list -> {
                    ticketTypes.setValue(list);
                    recomputeTotal();
                })
                .addOnFailureListener(e -> {
                    error.setValue(e != null ? e.getMessage() : "Không tải được danh sách vé.");
                });
    }

    /**
     * delta > 0: tăng, < 0: giảm
     * typeId = tickets_class (ví dụ "Premium", "VIP", "General", "STD", ...)
     * unitPriceIgnored: giữ lại tham số cũ cho compatible, nhưng không dùng nữa
     */
    public void changeQuantity(String typeId, long unitPriceIgnored, int delta) {
        int cur = qtyByType.getOrDefault(typeId, 0);
        int next = Math.max(0, cur + delta);
        qtyByType.put(typeId, next);
        recomputeTotal();
    }

    private void recomputeTotal() {
        long sum = 0L;
        List<TicketInfor> tt = ticketTypes.getValue();

        for (Map.Entry<String, Integer> e : qtyByType.entrySet()) {
            String typeId = e.getKey();   // tickets_class
            int q = e.getValue();
            long price = 0L;

            if (tt != null) {
                for (TicketInfor info : tt) {
                    // so sánh với tickets_class trong core model
                    if (info.getTickets_class() != null &&
                            info.getTickets_class().equalsIgnoreCase(typeId)) {
                        price = info.getTickets_price();
                        break;
                    }
                }
            }
            sum += (long) q * price;
        }
        totalPrice.setValue(sum);
    }

    // ====== Đặt vé: tạo Order trong Firestore (Orders collection) ======
    public void book(@NonNull String userId,
                     @NonNull String eventId,
                     @NonNull String showId) {

        if (qtyByType.isEmpty()) {
            error.setValue("Bạn chưa chọn vé nào.");
            return;
        }

        // Copy map để tránh thay đổi trong lúc Firestore đang chạy
        Map<String, Integer> qtyCopy = new HashMap<>(qtyByType);

        // BookingRepository.createOrder -> Order_API.createOrderForEvent
        // Order_API đã được sửa để:
        //  - đọc Tickets_infor
        //  - tạo Order
        //  - tăng tickets_sold bằng WriteBatch
        repo.createOrder(userId, eventId, showId, qtyCopy)
                .addOnSuccessListener(orderId -> {
                    String finalOrderId = orderId;
                    if (finalOrderId == null || finalOrderId.trim().isEmpty()) {
                        // Fallback demo nếu vì lý do nào đó orderId rỗng
                        finalOrderId = "ORD-" + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();
                    }

                    orderResult.setValue(new OrderResult(finalOrderId));

                    // reset state sau khi đặt
                    qtyByType.clear();
                    recomputeTotal();
                })
                .addOnFailureListener(e -> {
                    error.setValue(e != null ? e.getMessage() : "Không thể tạo đơn hàng.");
                });
    }
}
