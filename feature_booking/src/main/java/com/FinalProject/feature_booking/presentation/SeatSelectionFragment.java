package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.FinalProject.feature_booking.model.SeatState;
import com.FinalProject.feature_booking.model.TicketType;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SeatSelectionFragment extends Fragment {

    private static final int MAX_SELECT = 4;

    private String eventId, showId;
    private GridLayout grid;
    private TextView tvSelected, tvTotal;

    // Header & legend
    private TextView tvEventTitle;
    private TextView tvEventSubtitle;
    private TextView tvEventLocation;
    private TextView tvPricePremium;
    private TextView tvPriceVip;
    private TextView tvPriceGeneral;

    private View btnNext;

    private final Map<String, SeatState> stateBySeat = new HashMap<>();
    private final LinkedHashSet<String> selected     = new LinkedHashSet<>();
    private final Set<String> reservedSeats          = new HashSet<>();

    // ---------- Firestore pricing ----------
    private BookingRepository bookingRepo;
    private boolean pricesLoaded = false;

    // 3 mức giá theo zone (fallback = demo nếu không load được Firestore)
    private long priceStd  = 120_000L;  // hàng khác A/B
    private long priceVip  = 220_000L;  // hàng B
    private long priceVvip = 350_000L;  // hàng A

    private final NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public SeatSelectionFragment() {
        super(R.layout.fragment_seat_selection);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Header views
        tvEventTitle    = view.findViewById(R.id.tv_seat_event_title);
        tvEventSubtitle = view.findViewById(R.id.tv_seat_subtitle);
        tvEventLocation = view.findViewById(R.id.tv_seat_location);

        // Legend price views
        tvPricePremium  = view.findViewById(R.id.tv_price_premium);
        tvPriceVip      = view.findViewById(R.id.tv_price_vip);
        tvPriceGeneral  = view.findViewById(R.id.tv_price_general);

        grid       = view.findViewById(R.id.grid_seats);
        tvSelected = view.findViewById(R.id.tv_selected_seats);
        tvTotal    = view.findViewById(R.id.tv_total_price);
        btnNext    = view.findViewById(R.id.btn_proceed_checkout);

        bookingRepo = BookingRepository.getInstance();

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString("eventId", "");
            showId  = args.getString("showId", "");
            // Nếu EventDetailFragment có truyền eventTitle thì set tạm,
            // sau đó Firestore sẽ override cho nhất quán.
            String eventTitle = args.getString("eventTitle", null);
            if (tvEventTitle != null && !TextUtils.isEmpty(eventTitle)) {
                tvEventTitle.setText(eventTitle);
            }
        }

        // Load TicketType từ Firestore -> suy ra giá theo zone
        // -> đồng thời load header event (location, datetime)
        // -> sau đó load ghế reserved -> init seats.
        loadTicketTypesAndInitSeats();

        btnNext.setOnClickListener(v -> {
            if (selected.isEmpty()) return;

            ArrayList<String> list = new ArrayList<>(selected);
            long total = computeTotal();

            Bundle toCheckout = new Bundle();
            toCheckout.putString("eventId", eventId);
            toCheckout.putString("showId",  showId);
            toCheckout.putStringArray("selectedSeats", list.toArray(new String[0]));
            toCheckout.putLong("totalPrice", total);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_seatSelection_to_checkout, toCheckout);
        });

        renderSummary();
    }

    // ================== LOAD EVENT HEADER + GIÁ VÉ TỪ FIRESTORE ==================

    private void loadTicketTypesAndInitSeats() {
        // Fallback demo nếu eventId rỗng
        if (TextUtils.isEmpty(eventId)) {
            eventId = "seed_tedxyouth_2024";
        }

        // SeatSelection tự load event từ Firestore bằng eventId
        loadEventHeaderFromFirestore();

        bookingRepo.getTicketTypesForEvent(eventId)
                .addOnSuccessListener(types -> {
                    if (!isAdded()) return;

                    if (types != null && !types.isEmpty()) {
                        applyPricesFromTicketTypes(types);
                    } // else giữ nguyên giá demo
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không tải được giá vé từ server, đang dùng giá demo.",
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    // Cập nhật lại legend giá theo giá hiện đang dùng (Firestore hoặc fallback)
                    updateZonePriceLabels();

                    // Dù success hay fail thì vẫn cho phép chọn với giá hiện tại
                    pricesLoaded = true;
                    // Sau khi đã biết giá -> load danh sách ghế đã đặt
                    loadReservedSeatsFromFirestore();
                });
    }

    private void loadEventHeaderFromFirestore() {
        if (bookingRepo == null || TextUtils.isEmpty(eventId)) return;

        bookingRepo.getEventDocument(eventId)
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;

                    // Title
                    String name = doc.getString(StoreField.EventFields.EVENT_NAME);
                    if (!TextUtils.isEmpty(name) && tvEventTitle != null) {
                        tvEventTitle.setText(name);
                    }

                    // Location
                    String location = doc.getString(StoreField.EventFields.EVENT_LOCATION);
                    if (!TextUtils.isEmpty(location) && tvEventLocation != null) {
                        tvEventLocation.setText(location);
                    }

                    // Datetime (event_datetime hoặc date + time)
                    String datetime = doc.getString("event_datetime");
                    if (TextUtils.isEmpty(datetime)) {
                        String date = doc.getString(StoreField.EventFields.EVENT_DATE);
                        String time = doc.getString("event_time"); // nếu có field riêng
                        String sub = "";
                        if (!TextUtils.isEmpty(date) && !TextUtils.isEmpty(time)) {
                            sub = date + " • " + time;
                        } else if (!TextUtils.isEmpty(date)) {
                            sub = String.valueOf(date);
                        } else if (!TextUtils.isEmpty(time)) {
                            sub = String.valueOf(time);
                        }
                        datetime = sub;
                    }
                    if (!TextUtils.isEmpty(datetime) && tvEventSubtitle != null) {
                        tvEventSubtitle.setText(datetime);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Fail thì giữ text mặc định
                });
    }

    private void applyPricesFromTicketTypes(@NonNull List<TicketType> types) {
        if (types.isEmpty()) return;

        List<TicketType> sorted = new ArrayList<>(types);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getPrice(), b.getPrice()));
        // sorted: rẻ -> đắt
        long cheapest  = sorted.get(0).getPrice();
        long mid       = (sorted.size() > 1) ? sorted.get(1).getPrice() : cheapest;
        long expensive = sorted.get(sorted.size() - 1).getPrice();

        // Layout giả định: A = premium, B = VIP, còn lại = general
        priceStd  = cheapest;
        priceVip  = mid;
        priceVvip = expensive;
    }

    private void updateZonePriceLabels() {
        String premiumText = vnd.format(priceVvip);
        String vipText     = vnd.format(priceVip);
        String generalText = vnd.format(priceStd);

        if (tvPricePremium != null) {
            tvPricePremium.setText(premiumText);
        }
        if (tvPriceVip != null) {
            tvPriceVip.setText(vipText);
        }
        if (tvPriceGeneral != null) {
            tvPriceGeneral.setText(generalText);
        }
    }

    // ================== LOAD GHẾ ĐÃ RESERVED TỪ FIRESTORE ==================

    private void loadReservedSeatsFromFirestore() {
        if (!isAdded()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query q = db.collection(StoreField.ORDERS)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo(StoreField.OrderFields.IS_PAID, true);

        if (!TextUtils.isEmpty(showId)) {
            q = q.whereEqualTo("show_id", showId);
        }

        q.get()
                .addOnSuccessListener(snap -> {
                    reservedSeats.clear();
                    if (snap != null && !snap.isEmpty()) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Object seatsObj = doc.get("seats");
                            if (seatsObj instanceof List) {
                                for (Object s : (List<?>) seatsObj) {
                                    if (s != null) {
                                        reservedSeats.add(String.valueOf(s));
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không tải được trạng thái ghế, hiển thị mặc định.",
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    // Dù success hay fail -> tiến hành init seats với reservedSeats hiện có
                    initSeats();
                    renderSummary();
                });
    }

    // ================== SEATS & UI ==================

    /** Tính giá 1 ghế dựa trên hàng + giá zone đã load từ Firestore. */
    private long priceOf(@NonNull String seat) {
        if (seat.isEmpty()) return 0L;
        char row = Character.toUpperCase(seat.charAt(0));
        switch (row) {
            case 'A': return priceVvip;
            case 'B': return priceVip;
            default:  return priceStd;
        }
    }

    private void initSeats() {
        if (grid == null) return;

        int colCount = grid.getColumnCount();
        if (colCount <= 0) colCount = 6; // fallback an toàn

        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (!(child instanceof MaterialButton)) continue;

            MaterialButton btn = (MaterialButton) child;

            // Tắt checkable để tự quản lý state
            btn.setCheckable(false);
            btn.setChecked(false);

            // Ưu tiên dùng android:tag, nếu không có thì tự tính theo index
            Object t = child.getTag();
            final String seat = buildSeatId(i, colCount, t);

            // 🔹 Text hiển thị theo zone: "A3\nPremium", "B2\nVIP", "C5\nGeneral"
            btn.setText(buildSeatButtonLabel(seat));

            SeatState init = reservedSeats.contains(seat)
                    ? SeatState.RESERVED
                    : SeatState.AVAILABLE;

            stateBySeat.put(seat, init);
            applySeatStyle(btn, init);

            // Tap bình thường: chọn / bỏ chọn ghế
            btn.setOnClickListener(v -> onSeatClicked(seat, btn));

            // Long-press: mở bottom sheet thông tin ghế
            btn.setOnLongClickListener(v -> {
                showSeatInfoBottomSheet(seat);
                return true;
            });
        }
    }

    /**
     * Tạo mã ghế:
     *  - Nếu tag không rỗng -> dùng tag (A1, B2, ...)
     *  - Nếu tag null -> suy ra theo index + số cột: A1..A6, B1..B6, ...
     */
    @NonNull
    private String buildSeatId(int index, int colCount, @Nullable Object tag) {
        if (tag != null) {
            String s = tag.toString().trim();
            if (!s.isEmpty()) {
                return s.toUpperCase(Locale.ROOT);
            }
        }
        int row = index / colCount;  // 0-based
        int col = index % colCount;  // 0-based
        char rowChar = (char) ('A' + row); // row 0 -> 'A'
        return rowChar + String.valueOf(col + 1);
    }

    /** Label dùng cho text trên ghế trong grid (2 dòng). */
    @NonNull
    private String buildSeatButtonLabel(@NonNull String seat) {
        String zoneShort = zoneShortLabelForSeat(seat);
        if (TextUtils.isEmpty(zoneShort)) return seat;
        return seat ;
    }

    /**
     * Label ngắn hiển thị trên ghế:
     *  - A → Premium
     *  - B → VIP
     *  - C,D,... → General
     */
    @NonNull
    private String zoneShortLabelForSeat(@NonNull String seat) {
        if (seat.isEmpty()) return "";
        char row = Character.toUpperCase(seat.charAt(0));
        switch (row) {
            case 'A':
                return "Premium";
            case 'B':
                return "VIP";
            default:
                return "General";
        }
    }

    private void applySeatStyle(@NonNull MaterialButton btn, @NonNull SeatState st) {
        // Chặn trạng thái toggle mặc định
        btn.setCheckable(false);
        btn.setChecked(false);

        btn.setPressed(false);
        btn.setHovered(false);

        btn.setIcon(null);
        btn.setStrokeWidth(2);
        btn.setCornerRadius(14);

        switch (st) {
            case RESERVED:
                btn.setEnabled(false);
                btn.setBackgroundResource(R.drawable.bg_seat_reserved);
                btn.setTextColor(requireContext().getColor(android.R.color.darker_gray));
                break;

            case SELECTED:
                btn.setEnabled(true);
                btn.setBackgroundResource(R.drawable.bg_seat_selected);
                btn.setTextColor(requireContext().getColor(android.R.color.white));
                break;

            case AVAILABLE:
            default:
                btn.setEnabled(true);
                btn.setBackgroundResource(R.drawable.bg_seat_available);
                btn.setTextColor(0xFF222222);
                break;
        }

        btn.setRippleColorResource(android.R.color.transparent);
    }

    private void onSeatClicked(String seat, MaterialButton btn) {
        SeatState cur = stateBySeat.get(seat);
        if (cur == SeatState.RESERVED) return;

        // Chặn click nếu giá chưa load xong
        if (!pricesLoaded) {
            Snackbar.make(requireView(),
                    "Đang tải giá vé, vui lòng chờ một chút...",
                    Snackbar.LENGTH_SHORT).show();

            btn.setPressed(false);
            btn.setHovered(false);
            applySeatStyle(btn, cur == null ? SeatState.AVAILABLE : cur);
            return;
        }

        if (cur == SeatState.AVAILABLE) {
            if (selected.size() >= MAX_SELECT) {
                Snackbar.make(requireView(),
                        "Bạn chỉ có thể chọn tối đa " + MAX_SELECT + " ghế.",
                        Snackbar.LENGTH_SHORT).show();

                btn.setPressed(false);
                btn.setHovered(false);
                applySeatStyle(btn, SeatState.AVAILABLE);
                return;
            }
            stateBySeat.put(seat, SeatState.SELECTED);
            selected.add(seat);
            applySeatStyle(btn, SeatState.SELECTED);

        } else { // SELECTED -> bỏ chọn
            stateBySeat.put(seat, SeatState.AVAILABLE);
            selected.remove(seat);
            applySeatStyle(btn, SeatState.AVAILABLE);
        }
        renderSummary();
    }

    private long computeTotal() {
        long sum = 0L;
        for (String s : selected) {
            sum += priceOf(s);
        }
        return sum;
    }

    private void renderSummary() {
        if (!pricesLoaded) {
            if (tvSelected != null) {
                tvSelected.setText("Đang tải giá vé...");
            }
            if (tvTotal != null) {
                tvTotal.setText("Tổng tiền: đang tính...");
            }
            if (btnNext != null) btnNext.setEnabled(false);
            return;
        }

        if (tvSelected != null) {
            if (selected.isEmpty()) {
                tvSelected.setText("Chưa chọn ghế");
            } else {
                tvSelected.setText("Ghế: " + String.join(", ", selected));
            }
        }

        long total = computeTotal();
        if (tvTotal != null) {
            tvTotal.setText("Tổng tiền: " + vnd.format(total));
        }
        if (btnNext != null) btnNext.setEnabled(!selected.isEmpty());
    }

    // ================== Bottom sheet info ghế (long-press) ==================

    @NonNull
    private String zoneLabelForSeat(@NonNull String seat) {
        if (seat.isEmpty()) return "General";
        char row = Character.toUpperCase(seat.charAt(0));
        switch (row) {
            case 'A':
                return "Premium";
            case 'B':
                return "VIP";
            default:
                return "General";
        }
    }

    private void showSeatInfoBottomSheet(@NonNull String seat) {
        if (!isAdded()) return;

        if (!pricesLoaded) {
            Snackbar.make(requireView(),
                    "Đang tải giá vé, vui lòng chờ một chút...",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        long price = priceOf(seat);
        String zoneLabel = zoneLabelForSeat(seat);
        String priceStr = vnd.format(price);

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        );

        View content = getLayoutInflater().inflate(R.layout.bottomsheet_seat_info, null, false);
        TextView tvTitle = content.findViewById(R.id.tv_seat_info_title);
        TextView tvZone  = content.findViewById(R.id.tv_seat_info_zone);
        TextView tvPrice = content.findViewById(R.id.tv_seat_info_price);
        View btnClose    = content.findViewById(R.id.btn_seat_info_close);

        // 🔹 Chỉ hiện MÃ GHẾ ở title, ví dụ "A1"
        if (tvTitle != null) {
            tvTitle.setText(seat);
        }
        // 🔹 Dòng zone + giá vẫn giữ format có prefix cho dễ đọc
        if (tvZone != null) {
            tvZone.setText("Khu: " + zoneLabel);
        }
        if (tvPrice != null) {
            tvPrice.setText("Giá: " + priceStr);
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setContentView(content);
        dialog.show();
    }
}
