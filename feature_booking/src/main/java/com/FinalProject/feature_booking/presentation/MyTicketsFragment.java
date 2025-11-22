package com.FinalProject.feature_booking.presentation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.provider.CalendarContract;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

// ZXing
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.common.BitMatrix;

// Firestore
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * MyTicketsFragment – phiên bản dùng RecyclerView + Orders từ Firestore.
 *
 * - Dùng FirebaseAuthHelper để lấy userId.
 * - BookingRepository.getOrdersForUser(userId) để lấy danh sách Orders.
 * - Join sang Events/{event_id} để lấy tên sự kiện, event_date/event_start, event_location...
 * - Phân loại:
 *      Upcoming: event_date >= hôm nay (hoặc không parse được -> xem như upcoming)
 *      History : event_date < hôm nay
 * - Hiển thị:
 *      RecyclerView rv_tickets_upcoming
 *      RecyclerView rv_tickets_history
 * - Mỗi item sử dụng layout view_ticket_card.xml.
 */
public class MyTicketsFragment extends Fragment {

    private BookingRepository bookingRepo;

    // RecyclerView + Adapter
    private RecyclerView rvUpcoming, rvHistory;
    private TicketAdapter upcomingAdapter, historyAdapter;
    private View emptyView; // TextView hoặc layout trống, id: tv_empty_tickets

    // Dữ liệu cục bộ
    private final List<TicketItem> upcomingTickets = new ArrayList<>();
    private final List<TicketItem> historyTickets  = new ArrayList<>();

    private final NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public MyTicketsFragment() {
        super(R.layout.fragment_tickets);
    }

    private String fileProviderAuthority() {
        return requireContext().getPackageName() + ".fileprovider";
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        bookingRepo = BookingRepository.getInstance();

        // Chip filters
        ChipGroup chipGroup   = root.findViewById(R.id.chip_group_ticket_filters);
        Chip chipUpcoming     = root.findViewById(R.id.chip_ticket_upcoming);
        Chip chipHistoryTab   = root.findViewById(R.id.chip_ticket_history);
        Chip chipShared       = root.findViewById(R.id.chip_ticket_shared);

        // RecyclerView
        rvUpcoming = root.findViewById(R.id.rv_tickets_upcoming);
        rvHistory  = root.findViewById(R.id.rv_tickets_history);
        emptyView  = root.findViewById(R.id.tv_empty_tickets); // có thể null nếu bạn chưa tạo

        if (rvUpcoming != null) {
            rvUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
            upcomingAdapter = new TicketAdapter(true);
            rvUpcoming.setAdapter(upcomingAdapter);
        }

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
            historyAdapter = new TicketAdapter(false);
            rvHistory.setAdapter(historyAdapter);
        }

        // Filter chips: chỉ show list tương ứng
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int id = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);

                boolean showUpcoming = (id == R.id.chip_ticket_upcoming) || id == View.NO_ID;
                boolean showHistory  = (id == R.id.chip_ticket_history);
                boolean showShared   = (id == R.id.chip_ticket_shared);

                if (rvUpcoming != null) {
                    rvUpcoming.setVisibility(showUpcoming ? View.VISIBLE : View.GONE);
                }
                if (rvHistory != null) {
                    rvHistory.setVisibility(showHistory ? View.VISIBLE : View.GONE);
                }

                if (showShared) {
                    if (rvUpcoming != null) rvUpcoming.setVisibility(View.GONE);
                    if (rvHistory != null) rvHistory.setVisibility(View.GONE);
                    if (isAdded()) {
                        Snackbar.make(requireView(),
                                "Tab Chia sẻ: bạn có thể hiện thực list vé được chia sẻ sau.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }

                tintFilterChip(chipUpcoming,   id == R.id.chip_ticket_upcoming || id == View.NO_ID);
                tintFilterChip(chipHistoryTab, id == R.id.chip_ticket_history);
                tintFilterChip(chipShared,     id == R.id.chip_ticket_shared);
            });

            // Default: Upcoming
            tintFilterChip(chipUpcoming, true);
            tintFilterChip(chipHistoryTab, false);
            tintFilterChip(chipShared, false);
        }

        // 🔹 Load Orders từ Firestore theo user thật (chuẩn hoá qua FirebaseAuthHelper)
        loadMyTicketsFromFirestore();
    }

    // ---------------- Load Orders của user từ Firestore ----------------

    private void loadMyTicketsFromFirestore() {
        String userId = FirebaseAuthHelper.getCurrentUserUid();
        if (TextUtils.isEmpty(userId)) {
            if (rvUpcoming != null) rvUpcoming.setVisibility(View.GONE);
            if (rvHistory != null) rvHistory.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);

            if (isAdded()) {
                Snackbar.make(requireView(),
                        "Bạn cần đăng nhập để xem vé đã mua.",
                        Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        bookingRepo.getOrdersForUser(userId)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    if (snap == null || snap.isEmpty()) {
                        upcomingTickets.clear();
                        historyTickets.clear();
                        if (upcomingAdapter != null) {
                            upcomingAdapter.submitList(new ArrayList<>());
                        }
                        if (historyAdapter != null) {
                            historyAdapter.submitList(new ArrayList<>());
                        }
                        if (rvUpcoming != null) rvUpcoming.setVisibility(View.GONE);
                        if (rvHistory != null) rvHistory.setVisibility(View.GONE);
                        if (emptyView != null) emptyView.setVisibility(View.VISIBLE);

                        Snackbar.make(requireView(),
                                "Bạn chưa có vé nào. Hãy đặt vé để xem ở đây.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    bindOrdersSnapshot(snap);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không thể tải danh sách vé: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    /**
     * Duyệt toàn bộ Orders của user:
     * - Với mỗi Order, join sang Events/{event_id}
     * - Tạo TicketItem
     * - Phân loại Upcoming/History theo event_date / event_start
     */
    private void bindOrdersSnapshot(@NonNull QuerySnapshot snap) {
        upcomingTickets.clear();
        historyTickets.clear();

        List<DocumentSnapshot> docs = snap.getDocuments();
        if (docs.isEmpty()) {
            if (rvUpcoming != null) rvUpcoming.setVisibility(View.GONE);
            if (rvHistory != null) rvHistory.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
            return;
        }

        if (emptyView != null) emptyView.setVisibility(View.GONE);

        for (DocumentSnapshot doc : docs) {
            final DocumentSnapshot orderSnapshot = doc;
            String eventId = orderSnapshot.getString("event_id");

            if (TextUtils.isEmpty(eventId)) {
                // Không có event_id -> vẫn tạo item, xem như Upcoming
                TicketItem item = buildTicketItem(orderSnapshot, null);
                addTicketAndRefresh(item);
            } else {
                bookingRepo.getEventDocument(eventId)
                        .addOnSuccessListener(eventDoc -> {
                            if (!isAdded()) return;
                            TicketItem item = buildTicketItem(orderSnapshot, eventDoc);
                            addTicketAndRefresh(item);
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            // Nếu join event fail -> vẫn hiển thị item nhưng thiếu thông tin event
                            TicketItem item = buildTicketItem(orderSnapshot, null);
                            addTicketAndRefresh(item);
                        });
            }
        }
    }

    /**
     * Thêm 1 TicketItem vào list Upcoming/History và cập nhật adapter.
     */
    private void addTicketAndRefresh(@NonNull TicketItem item) {
        long now = System.currentTimeMillis();
        boolean isUpcoming = (item.eventTimeMillis <= 0L) || (item.eventTimeMillis >= now);
        item.upcoming = isUpcoming;

        if (isUpcoming) {
            upcomingTickets.add(item);
        } else {
            historyTickets.add(item);
        }

        sortTickets();

        if (upcomingAdapter != null) {
            upcomingAdapter.submitList(new ArrayList<>(upcomingTickets));
        }
        if (historyAdapter != null) {
            historyAdapter.submitList(new ArrayList<>(historyTickets));
        }

        if (emptyView != null) {
            if (upcomingTickets.isEmpty() && historyTickets.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Sắp xếp:
     * - Upcoming: eventDate tăng dần (gần nhất lên trước)
     * - History : eventDate giảm dần (mới diễn ra gần nhất lên trước)
     */
    private void sortTickets() {
        Collections.sort(upcomingTickets, (a, b) -> {
            long ta = (a.eventTimeMillis <= 0) ? Long.MAX_VALUE : a.eventTimeMillis;
            long tb = (b.eventTimeMillis <= 0) ? Long.MAX_VALUE : b.eventTimeMillis;
            return Long.compare(ta, tb);
        });

        Collections.sort(historyTickets, (a, b) -> {
            long ta = (a.eventTimeMillis <= 0) ? Long.MIN_VALUE : a.eventTimeMillis;
            long tb = (b.eventTimeMillis <= 0) ? Long.MIN_VALUE : b.eventTimeMillis;
            return Long.compare(tb, ta); // DESC
        });
    }

    // ---------------- TicketItem (UI model) ----------------

    private static class TicketItem {
        String orderId;
        String eventId;
        String showId;

        String eventTitle;
        String showInfo;       // datetime • venue
        String ticketSummary;  // "VIP x2, STD x1"
        long   totalPrice;
        boolean paid;
        String paymentMethodLabel;

        long eventTimeMillis;  // từ event_date / event_start (dùng để phân loại upcoming/history)
        boolean upcoming;

        // 🔹 payload QR lưu trong Order (field "qr_code")
        String qrPayload;
    }

    /**
     * Build TicketItem từ Order + Event (có thể null nếu join lỗi).
     */
    @NonNull
    private TicketItem buildTicketItem(@NonNull DocumentSnapshot orderDoc,
                                       @Nullable DocumentSnapshot eventDoc) {
        TicketItem item = new TicketItem();

        item.orderId = orderDoc.getId();
        item.eventId = orderDoc.getString("event_id");
        item.showId  = orderDoc.getString("show_id");

        Long totalPriceLong  = orderDoc.getLong(StoreField.OrderFields.TOTAL_PRICE);
        Boolean isPaid       = orderDoc.getBoolean(StoreField.OrderFields.IS_PAID);
        String paymentMethod = orderDoc.getString(StoreField.OrderFields.PAYMENT_METHOD);
        String qrCodeStr     = orderDoc.getString("qr_code");

        item.totalPrice = (totalPriceLong != null) ? totalPriceLong : 0L;
        item.paid       = (isPaid != null) && isPaid;
        item.paymentMethodLabel =
                !TextUtils.isEmpty(paymentMethod)
                        ? paymentMethod
                        : (item.paid ? "ĐÃ THANH TOÁN" : "CHƯA THANH TOÁN");

        // Lưu payload QR (nếu có)
        item.qrPayload = !TextUtils.isEmpty(qrCodeStr) ? qrCodeStr : "";

        // ticket_items -> summary
        List<?> rawItems = (List<?>) orderDoc.get(StoreField.OrderFields.TICKET_ITEMS);
        item.ticketSummary = buildTicketSummary(rawItems);

        // Event info
        String eventTitle = null;
        String venue      = null;
        String dateTime   = null;

        if (eventDoc != null && eventDoc.exists()) {
            eventTitle = safeGetString(eventDoc, StoreField.EventFields.EVENT_NAME);
            venue      = safeGetString(eventDoc, StoreField.EventFields.EVENT_LOCATION);

            // hiển thị: ưu tiên event_datetime, fallback EVENT_DATE, cuối cùng event_start (Seeder)
            dateTime   = safeGetString(eventDoc, "event_datetime");
            if (TextUtils.isEmpty(dateTime)) {
                dateTime = safeGetString(eventDoc, StoreField.EventFields.EVENT_DATE);
            }
            if (TextUtils.isEmpty(dateTime)) {
                dateTime = safeGetString(eventDoc, "event_start");
            }

            item.eventTimeMillis = extractEventDateMillis(eventDoc);
        } else {
            item.eventTimeMillis = 0L;
        }

        if (TextUtils.isEmpty(eventTitle)) {
            eventTitle = "Sự kiện";
        }
        item.eventTitle = eventTitle;

        String showInfo;
        if (!TextUtils.isEmpty(dateTime) && !TextUtils.isEmpty(venue)) {
            showInfo = dateTime + " • " + venue;
        } else if (!TextUtils.isEmpty(dateTime)) {
            showInfo = dateTime;
        } else if (!TextUtils.isEmpty(venue)) {
            showInfo = venue;
        } else if (!TextUtils.isEmpty(item.showId)) {
            showInfo = "Suất diễn: " + item.showId;
        } else {
            showInfo = "Suất diễn: đang cập nhật";
        }
        item.showInfo = showInfo;

        return item;
    }

    /**
     * Cố gắng đọc thời gian sự kiện từ DocumentSnapshot và convert sang millis để so sánh.
     * Ưu tiên:
     *   - EVENT_DATE
     *   - event_start
     *   - event_datetime
     *   - Kiểu Date / Timestamp / String ("yyyy-MM-dd", "dd/MM/yyyy", ISO-8601)
     */
    private long extractEventDateMillis(@Nullable DocumentSnapshot eventDoc) {
        if (eventDoc == null) return 0L;

        Object dateObj = eventDoc.get(StoreField.EventFields.EVENT_DATE);
        if (dateObj == null) dateObj = eventDoc.get("event_start");
        if (dateObj == null) dateObj = eventDoc.get("event_datetime");
        if (dateObj == null) return 0L;

        try {
            if (dateObj instanceof Date) {
                return ((Date) dateObj).getTime();
            }
            // Firestore Timestamp
            if (dateObj instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) dateObj).toDate().getTime();
            }
            if (dateObj instanceof String) {
                String s = ((String) dateObj).trim();

                // ISO-8601: 2025-12-10T19:00:00Z (Seeder)
                try {
                    SimpleDateFormat isoZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    isoZ.setLenient(true);
                    return isoZ.parse(s).getTime();
                } catch (ParseException ignored) {}

                // yyyy-MM-dd
                try {
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    iso.setLenient(false);
                    return iso.parse(s).getTime();
                } catch (ParseException ignored) {}

                // dd/MM/yyyy
                try {
                    SimpleDateFormat vn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    vn.setLenient(false);
                    return vn.parse(s).getTime();
                } catch (ParseException ignored) {}
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }

    // ---------------- TicketAdapter (RecyclerView) ----------------

    private class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketVH> {

        private final List<TicketItem> items = new ArrayList<>();
        private final boolean isUpcomingList;

        TicketAdapter(boolean isUpcomingList) {
            this.isUpcomingList = isUpcomingList;
        }

        void submitList(@NonNull List<TicketItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TicketVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_ticket_card, parent, false);
            return new TicketVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TicketVH holder, int position) {
            holder.bind(items.get(position), isUpcomingList);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class TicketVH extends RecyclerView.ViewHolder {

            TextView tvTitle, tvShowInfo, tvSeats, tvOrder;
            Chip chipStatus;
            MaterialButton btnQr, btnDetail, btnCalendar;

            TicketVH(@NonNull View itemView) {
                super(itemView);
                tvTitle      = itemView.findViewById(R.id.tv_event_title);
                tvShowInfo   = itemView.findViewById(R.id.tv_show_info);
                tvSeats      = itemView.findViewById(R.id.tv_seats);
                tvOrder      = itemView.findViewById(R.id.tv_order);
                chipStatus   = itemView.findViewById(R.id.chip_status);
                btnQr        = itemView.findViewById(R.id.btn_view_qr);
                btnDetail    = itemView.findViewById(R.id.btn_view_detail);
                btnCalendar  = itemView.findViewById(R.id.btn_add_calendar);

                safeSetIcon(btnQr,       R.drawable.ic_qr_code_24);
                safeSetIcon(btnDetail,   R.drawable.ic_info_24);
                safeSetIcon(btnCalendar, R.drawable.ic_event_add_24);
            }

            void bind(@NonNull TicketItem item, boolean upcomingList) {
                String orderId        = item.orderId;
                String eventTitle     = item.eventTitle;
                String showInfo       = item.showInfo;
                String ticketSummary  = item.ticketSummary;
                long totalPrice       = item.totalPrice;
                boolean paid          = item.paid;
                String paymentLabel   = item.paymentMethodLabel;

                if (tvTitle != null) tvTitle.setText(eventTitle);
                if (tvShowInfo != null) {
                    if (!TextUtils.isEmpty(showInfo)) {
                        tvShowInfo.setText(showInfo);
                    } else if (!TextUtils.isEmpty(item.showId)) {
                        tvShowInfo.setText("Suất diễn: " + item.showId);
                    } else {
                        tvShowInfo.setText("Suất diễn: đang cập nhật");
                    }
                }
                if (tvSeats != null) {
                    tvSeats.setText("Vé: " + (TextUtils.isEmpty(ticketSummary) ? "-" : ticketSummary));
                }
                if (tvOrder != null) {
                    tvOrder.setText("Mã GD: " + orderId + "\nTổng: " + vnd.format(totalPrice));
                }
                if (chipStatus != null) {
                    chipStatus.setText(paid ? (upcomingList ? "UPCOMING" : "HISTORY") : "UNPAID");
                    styleStatusChip(chipStatus, upcomingList);
                }

                // Xem QR
                if (btnQr != null) {
                    btnQr.setEnabled(true);
                    btnQr.setAlpha(1f);
                    btnQr.setOnClickListener(v -> {
                        String eTitle = tvTitle != null ? tvTitle.getText().toString() : eventTitle;
                        String sInfo  = tvShowInfo != null ? tvShowInfo.getText().toString() : showInfo;
                        showTicketQr(item, eTitle, ticketSummary, sInfo);
                    });
                }

                // Chi tiết vé (TicketDetailFragment) – Firestore-driven bằng orderId
                if (btnDetail != null) {
                    btnDetail.setOnClickListener(v -> {
                        String eTitle = tvTitle != null ? tvTitle.getText().toString() : eventTitle;
                        String sInfo  = tvShowInfo != null ? tvShowInfo.getText().toString() : showInfo;

                        navigateToTicketDetailFromOrder(
                                orderId,
                                eTitle,
                                ticketSummary,
                                totalPrice,
                                paymentLabel
                        );
                    });
                }

                // Thêm vào lịch (demo)
                if (btnCalendar != null) {
                    btnCalendar.setOnClickListener(v -> {
                        String eTitle = tvTitle != null ? tvTitle.getText().toString() : eventTitle;
                        long now = System.currentTimeMillis();
                        addToCalendar(
                                eTitle,
                                "Sự kiện đã mua vé",
                                now + 3_600_000L,
                                now + 5_400_000L,
                                "HCM"
                        );
                    });
                }
            }
        }
    }

    // ---------------- Build ticket summary từ ticket_items ----------------

    @SuppressWarnings("unchecked")
    @NonNull
    private String buildTicketSummary(@Nullable List<?> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) return "-";

        StringBuilder sb = new StringBuilder();
        for (Object o : rawItems) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) o;

            String cls = (String) m.get("tickets_class");
            if (cls == null) {
                cls = (String) m.get("tickets_infor_id");
            }

            Number qNum = (Number) m.get("quantity");
            int q = qNum != null ? qNum.intValue() : 0;
            if (q <= 0 || cls == null) continue;

            if (sb.length() > 0) sb.append(", ");
            sb.append(cls).append(" x").append(q);
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    // ---------- Điều hướng sang TicketDetailFragment (dựa trên orderId) ----------

    private void navigateToTicketDetailFromOrder(@NonNull String orderId,
                                                 @NonNull String eventTitle,
                                                 @NonNull String ticketSummary,
                                                 long totalPaid,
                                                 @NonNull String method) {
        Bundle b = new Bundle();
        // Args mới cho Firestore
        b.putString("orderId", orderId);

        // Fallback cũ (TicketDetailFragment vẫn đọc được nếu bạn chưa sửa hết)
        b.putString("ticket_event_title", eventTitle);
        b.putStringArray("ticket_selected_seats", new String[]{ ticketSummary });
        b.putInt("ticket_total_price", (int) Math.min(totalPaid, Integer.MAX_VALUE));
        b.putString("ticket_payment_method", method);

        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.ticketDetailFragment, b);
        } catch (Exception e) {
            if (!isAdded()) return;
            Snackbar.make(requireView(),
                    "Không thể mở chi tiết vé.",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    // ---------- QR: BottomSheet + generate + SHARE IMAGE ----------

    private void showTicketQr(@NonNull TicketItem item,
                              @NonNull String eventTitle,
                              @NonNull String seatsSummary,
                              @NonNull String showInfo) {
        if (!isAdded()) return;

        // 🔹 Ưu tiên payload từ Firestore (field qr_code đã lưu khi createOrder)
        String payload = item.qrPayload;
        if (TextUtils.isEmpty(payload)) {
            // Fallback: build JSON đơn giản cho các đơn cũ chưa có qr_code
            payload = "{"
                    + "\"ticketId\":\"" + safe(item.orderId) + "\","
                    + "\"event\":\""    + safe(eventTitle) + "\","
                    + "\"summary\":\""  + safe(seatsSummary) + "\","
                    + "\"show\":\""     + safe(showInfo) + "\""
                    + "}";
        }

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        );
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottomsheet_qr_ticket, null, false);

        TextView tvTitle = content.findViewById(R.id.tv_qr_title);
        TextView tvMeta1 = content.findViewById(R.id.tv_qr_meta1);
        TextView tvMeta2 = content.findViewById(R.id.tv_qr_meta2);
        ImageView ivQr   = content.findViewById(R.id.iv_qr);
        MaterialButton btnShare = content.findViewById(R.id.btn_share_qr);
        MaterialButton btnClose = content.findViewById(R.id.btn_close_qr);
        View btnCloseHeader     = content.findViewById(R.id.btn_close_header);

        tvTitle.setText("Mã QR vé");
        tvMeta1.setText(eventTitle);
        tvMeta2.setText((seatsSummary.isEmpty() ? "" : "Vé: " + seatsSummary) +
                (showInfo.isEmpty() ? "" : (seatsSummary.isEmpty() ? "" : " • ") + showInfo));

        Bitmap bmp = createQrBitmap(payload, dp(264));
        if (bmp != null) ivQr.setImageBitmap(bmp);

        btnShare.setOnClickListener(v -> {
            if (bmp != null) {
                shareQrBitmap(bmp);
            } else {
                Snackbar.make(requireView(), "QR chưa sẵn sàng.", Snackbar.LENGTH_SHORT).show();
            }
        });

        View.OnClickListener doClose = vv -> dialog.dismiss();
        btnClose.setOnClickListener(doClose);
        if (btnCloseHeader != null) btnCloseHeader.setOnClickListener(doClose);

        dialog.setContentView(content);
        dialog.setCancelable(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }

    private void shareQrBitmap(@NonNull Bitmap bmp) {
        String fname = "qr_" + System.currentTimeMillis() + ".png";
        Uri uri = saveBitmapToCache(bmp, fname);
        if (uri == null) {
            if (isAdded()) {
                Snackbar.make(requireView(),
                        "Không thể lưu ảnh QR để chia sẻ.",
                        Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/png");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(android.content.ClipData.newRawUri("qr", uri));

        try {
            startActivity(Intent.createChooser(send, "Chia sẻ mã QR"));
        } catch (Exception e) {
            if (!isAdded()) return;
            Snackbar.make(requireView(), "Không thể chia sẻ ảnh.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Uri saveBitmapToCache(@NonNull Bitmap bmp, @NonNull String fileName) {
        try {
            File dir = new File(requireContext().getCacheDir(), "share");
            if (!dir.exists() && !dir.mkdirs()) return null;

            File out = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }

            return FileProvider.getUriForFile(
                    requireContext(),
                    fileProviderAuthority(),
                    out
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap createQrBitmap(@NonNull String data, int pxSize) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, pxSize, pxSize, hints);
            Bitmap bmp = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < pxSize; x++) {
                for (int y = 0; y < pxSize; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }

    // ---------- UI helpers ----------

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    private void safeSetIcon(@Nullable MaterialButton b, int res) {
        if (b == null) return;
        try {
            b.setIconResource(res);
        } catch (Exception ignored) {}
    }

    private void styleStatusChip(@NonNull Chip chip, boolean upcoming) {
        int bgAttr = upcoming
                ? com.google.android.material.R.attr.colorSecondaryContainer
                : com.google.android.material.R.attr.colorSurfaceVariant;
        int fgAttr = upcoming
                ? com.google.android.material.R.attr.colorOnSecondaryContainer
                : com.google.android.material.R.attr.colorOnSurfaceVariant;

        int bg = MaterialColors.getColor(chip, bgAttr);
        int fg = MaterialColors.getColor(chip, fgAttr);
        chip.setChipBackgroundColor(ColorStateList.valueOf(bg));
        chip.setTextColor(fg);
    }

    private void tintFilterChip(@Nullable Chip chip, boolean selected) {
        if (chip == null) return;
        int bg = MaterialColors.getColor(chip, selected
                ? com.google.android.material.R.attr.colorSecondaryContainer
                : com.google.android.material.R.attr.colorSurfaceVariant);
        int fg = MaterialColors.getColor(chip, selected
                ? com.google.android.material.R.attr.colorOnSecondaryContainer
                : com.google.android.material.R.attr.colorOnSurfaceVariant);
        chip.setChipBackgroundColor(ColorStateList.valueOf(bg));
        chip.setTextColor(fg);
    }

    private void addToCalendar(String title, String desc,
                               long startMillis, long endMillis,
                               String location) {
        Intent insert = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, desc)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
        try {
            startActivity(insert);
        } catch (Exception e) {
            if (!isAdded()) return;
            Snackbar.make(requireView(),
                    "Không thể mở ứng dụng Lịch.",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

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
