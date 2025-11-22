package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Màn chi tiết sự kiện cho flow booking.
 *
 * Layout: fragment_event_detail.xml (bản bạn gửi cuối cùng)
 *   - Dynamic:
 *       + tv_event_title
 *       + tv_event_datetime
 *       + tv_event_venue
 *       + tv_event_price_range
 *       + btn_choose_seat
 *
 *   - Các phần mô tả / lịch trình / lợi ích VIP hiện đang là text tĩnh trong XML.
 *
 * Logic:
 *   - Nhận eventId / eventTitle / showId từ NavArgs.
 *   - Dùng BookingRepository (Firestore) để:
 *        + load Events/{eventId} => event_name, event_location, event_date / event_datetime
 *        + load Events/{eventId}/Tickets_infor => tickets_price min–max
 *   - Bấm "Chọn chỗ ngồi" => sang SeatSelectionFragment.
 */
public class EventDetailFragment extends Fragment {

    private BookingRepository bookingRepo;

    private String eventId;
    private String eventTitleArg;
    private String showId;

    private TextView tvTitle;
    private TextView tvVenue;
    private TextView tvDateTime;
    private TextView tvPriceRange;
    private MaterialButton btnChooseSeat;

    private final NumberFormat vnd =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public EventDetailFragment() {
        super(R.layout.fragment_event_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepo   = BookingRepository.getInstance();

        tvTitle       = view.findViewById(R.id.tv_event_title);
        tvVenue       = view.findViewById(R.id.tv_event_venue);
        tvDateTime    = view.findViewById(R.id.tv_event_datetime);
        tvPriceRange  = view.findViewById(R.id.tv_event_price_range);
        btnChooseSeat = view.findViewById(R.id.btn_choose_seat);

        // ===== Đọc arguments từ NavGraph =====
        Bundle args = getArguments();
        eventId       = args != null ? args.getString("eventId", "") : "";
        eventTitleArg = args != null ? args.getString("eventTitle", "") : "";
        showId        = args != null ? args.getString("showId", "") : "";

        // Fallback demo nếu thiếu
        if (TextUtils.isEmpty(eventId)) {
            eventId = "seed_tedxyouth_2024";
        }
        if (TextUtils.isEmpty(eventTitleArg)) {
            eventTitleArg = "TEDxYouth Saigon 2024";
        }
        if (TextUtils.isEmpty(showId)) {
            showId = eventId + "_DEFAULT";
        }

        // Hiển thị tạm title trong args trong lúc chờ Firestore
        if (tvTitle != null) {
            tvTitle.setText(eventTitleArg);
        }

        // ===== Load dữ liệu thật từ Firestore =====
        loadEventFromFirestore();
        loadTicketSummaryFromFirestore();

        // ===== Điều hướng chọn chỗ ngồi =====
        if (btnChooseSeat != null) {
            btnChooseSeat.setOnClickListener(v -> {
                String finalTitle = tvTitle != null
                        ? tvTitle.getText().toString()
                        : eventTitleArg;

                Bundle toSeat = new Bundle();
                toSeat.putString("eventId", eventId);
                toSeat.putString("eventTitle", finalTitle);
                toSeat.putString("showId", showId);

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_eventDetail_to_seatSelection, toSeat);
            });
        }
    }

    // ============= Firestore: load Events/{eventId} =============

    private void loadEventFromFirestore() {
        bookingRepo.getEventDocument(eventId)
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;
                    bindEventDocument(doc);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không tải được thông tin sự kiện: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void bindEventDocument(@NonNull DocumentSnapshot doc) {
        // Tên sự kiện
        String name = safeGetString(doc, StoreField.EventFields.EVENT_NAME);
        if (!TextUtils.isEmpty(name) && tvTitle != null) {
            tvTitle.setText(name);
        }

        // Địa điểm: event_location
        String venue = safeGetString(doc, StoreField.EventFields.EVENT_LOCATION);
        if (tvVenue != null && !TextUtils.isEmpty(venue)) {
            tvVenue.setText(venue);
        }

        // Thời gian – ưu tiên event_datetime, fallback event_date
        String datetime = safeGetString(doc, "event_datetime");
        if (TextUtils.isEmpty(datetime)) {
            datetime = safeGetString(doc, StoreField.EventFields.EVENT_DATE);
        }
        if (tvDateTime != null && !TextUtils.isEmpty(datetime)) {
            tvDateTime.setText(datetime);
        }
    }

    // ============= Firestore: Tickets_infor (giá min–max) =============

    private void loadTicketSummaryFromFirestore() {
        bookingRepo.getTicketInfos(eventId)
                .addOnSuccessListener(snap -> {
                    if (!isAdded() || snap == null || snap.isEmpty()) {
                        // Không có dữ liệu -> giữ text "Giá vé: đang cập nhật"
                        return;
                    }
                    bindTicketSummary(snap);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không tải được giá vé: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void bindTicketSummary(@NonNull QuerySnapshot snap) {
        long minPrice = Long.MAX_VALUE;
        long maxPrice = Long.MIN_VALUE;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            Long priceLong = doc.getLong(StoreField.TicketFields.TICKETS_PRICE);
            if (priceLong == null) continue;
            long price = priceLong;

            if (price < minPrice) minPrice = price;
            if (price > maxPrice) maxPrice = price;
        }

        if (tvPriceRange == null) return;

        if (minPrice == Long.MAX_VALUE) {
            // Không có giá hợp lệ -> giữ nguyên text mặc định
            return;
        }

        if (minPrice == maxPrice) {
            tvPriceRange.setText("Giá vé: " + vnd.format(minPrice));
        } else {
            tvPriceRange.setText(
                    "Giá vé: " + vnd.format(minPrice) + " - " + vnd.format(maxPrice)
            );
        }
    }

    // ============= Helpers =============

    @Nullable
    private String safeGetString(@NonNull DocumentSnapshot doc, @NonNull String field) {
        try {
            Object v = doc.get(field);
            return v != null ? String.valueOf(v) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
