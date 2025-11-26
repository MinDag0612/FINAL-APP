package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.FinalProject.feature_booking.model.SeatState;
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

    private static final String TAG = "SeatSelection";

    private static final int MAX_SELECT = 4;
    private static final int DEFAULT_COLUMN_COUNT = 8;

    // page = số ghế generate mỗi lần (8 cột * 4 hàng = 32 ghế)
    private static final int PAGE_ROWS = 4;
    private static final int PAGE_SIZE = DEFAULT_COLUMN_COUNT * PAGE_ROWS;

    // Khi còn <= 2 hàng nữa là gần đáy -> load thêm
    private static final int LOAD_MORE_THRESHOLD = DEFAULT_COLUMN_COUNT * 2;

    // ========== QUOTA THEO HẠNG (NHẬN TỪ MÀN TRƯỚC) ==========
    // Hàng A -> Premium, B -> VIP, C... -> General
    private int quotaPremium = 0;   // số ghế Premium (A) user đã mua
    private int quotaVip     = 0;   // số ghế VIP (B)
    private int quotaGeneral = 0;   // số ghế General (C..)

    // Counter số ghế đang được chọn theo từng hạng
    private int selectedPremiumCount = 0;
    private int selectedVipCount     = 0;
    private int selectedGeneralCount = 0;

    private enum SeatCategory {
        PREMIUM,
        VIP,
        GENERAL
    }

    private static class SeatItem {
        final String seatId;
        final SeatCategory category;
        SeatState state;

        SeatItem(@NonNull String seatId,
                 @NonNull SeatCategory category,
                 @NonNull SeatState state) {
            this.seatId = seatId;
            this.category = category;
            this.state = state;
        }
    }

    private String eventId, showId;

    private RecyclerView rvSeats;
    private SeatAdapter seatAdapter;

    private TextView tvSelected, tvTotal;

    // Header & legend
    private TextView tvEventTitle;
    private TextView tvEventSubtitle;
    private TextView tvEventLocation;
    private TextView tvPricePremium;
    private TextView tvPriceVip;
    private TextView tvPriceGeneral;

    private View btnNext;

    // Trạng thái ghế
    private final Map<String, SeatState> stateBySeat = new HashMap<>();
    private final LinkedHashSet<String> selected     = new LinkedHashSet<>();
    private final Set<String> reservedSeats          = new HashSet<>();
    private final List<SeatItem> seatItems           = new ArrayList<>();

    // ---------- Firestore pricing ----------
    private BookingRepository bookingRepo;
    private boolean pricesLoaded = false;

    // 3 mức giá theo zone (fallback = demo nếu không load được Firestore)
    private long priceStd  = 120_000L;  // General
    private long priceVip  = 220_000L;  // VIP (B)
    private long priceVvip = 350_000L;  // Premium (A)

    // Số ghế theo từng loại (đọc từ TicketInfor.tickets_quantity)
    // Default ban đầu: A:6, B:6, C:12 (sẽ bị override bởi Firestore)
    private int seatsPremium  = 6;
    private int seatsVip      = 6;
    private int seatsGeneral  = 12;

    // tổng số ghế (A + B + C)
    private int totalSeatCount = 0;

    // số ghế đã generate vào seatItems (lazy load)
    private int generatedSeatCount = 0;
    private boolean isLoadingMore = false;

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

        rvSeats    = view.findViewById(R.id.rv_seats);
        tvSelected = view.findViewById(R.id.tv_selected_seats);
        tvTotal    = view.findViewById(R.id.tv_total_price);
        btnNext    = view.findViewById(R.id.btn_proceed_checkout);

        // RecyclerView + GridLayoutManager
        GridLayoutManager lm = new GridLayoutManager(requireContext(), DEFAULT_COLUMN_COUNT);
        rvSeats.setLayoutManager(lm);
        rvSeats.setHasFixedSize(true);
        rvSeats.setItemAnimator(null);           // tránh flicker khi notifyItemChanged
        rvSeats.setItemViewCacheSize(64);        // cache thêm view, scroll mượt hơn
        rvSeats.setNestedScrollingEnabled(false);// nếu sau này fragment nằm trong ScrollView thì vẫn ok


        seatAdapter = new SeatAdapter();
        rvSeats.setAdapter(seatAdapter);

        // Scroll listener để lazy load ghế
        rvSeats.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return; // chỉ quan tâm scroll xuống

                GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
                if (glm == null) return;

                int visibleItemCount = glm.getChildCount();
                int totalItemCount   = glm.getItemCount();
                int firstVisible     = glm.findFirstVisibleItemPosition();

                Log.v(TAG, "onScrolled: first=" + firstVisible
                        + ", visible=" + visibleItemCount
                        + ", total=" + totalItemCount
                        + ", generated=" + generatedSeatCount
                        + ", totalSeatCount=" + totalSeatCount);

                // khi xuống tới gần cuối list hiện tại thì load thêm
                if (!isLoadingMore
                        && generatedSeatCount < totalSeatCount
                        && (visibleItemCount + firstVisible) >= totalItemCount - LOAD_MORE_THRESHOLD) {
                    Log.d(TAG, "onScrolled: trigger loadNextPage()");
                    loadNextPage();
                }
            }
        });

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

            // Nhận quota từng hạng từ EventDetailFragment
            quotaPremium = args.getInt("qtyPremium", 0);
            quotaVip     = args.getInt("qtyVip", 0);
            quotaGeneral = args.getInt("qtyGeneral", 0);
        }

        // Load TicketType từ Firestore -> suy ra GIÁ + SỐ GHẾ mỗi hạng
        // -> load header event
        // -> sau đó load ghế reserved -> build seat map lazy.
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

    // ================== LOAD EVENT HEADER + GIÁ & SỐ GHẾ TỪ FIRESTORE ==================

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
                        applyPricesAndSeatCountFromTicketTypes(types);
                    } else {
                        Log.w(TAG, "loadTicketTypesAndInitSeats: ticketTypes null/empty, dùng layout default.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Không tải được cấu hình ticketTypes", e);
                    Snackbar.make(requireView(),
                            "Không tải được cấu hình vé, đang dùng giá & sơ đồ demo.",
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    // Cập nhật lại legend giá theo giá hiện đang dùng (Firestore hoặc fallback)
                    updateZonePriceLabels();

                    // set tổng ghế (nếu chưa set)
                    if (totalSeatCount <= 0) {
                        totalSeatCount = seatsPremium + seatsVip + seatsGeneral;
                    }

                    Log.d(TAG, "loadTicketTypesAndInitSeats: totalSeatCount=" + totalSeatCount);

                    // Dù success hay fail thì vẫn cho phép chọn với giá hiện tại
                    pricesLoaded = true;
                    // Sau khi đã biết GIÁ + SỐ GHẾ -> load danh sách ghế đã đặt
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
                    Log.e(TAG, "loadEventHeaderFromFirestore: fail", e);
                    // Fail thì giữ text mặc định
                });
    }

    /**
     * Đọc từ TicketInfor:
     *  - Sắp xếp theo giá: rẻ nhất = General, đắt nhất = Premium, còn lại = VIP.
     *  - Gán:
     *      + priceStd / priceVip / priceVvip
     *      + seatsGeneral / seatsVip / seatsPremium (từ tickets_quantity - tickets_sold)
     */
    private void applyPricesAndSeatCountFromTicketTypes(@NonNull List<TicketInfor> types) {
        if (types.isEmpty()) return;

        List<TicketInfor> sorted = new ArrayList<>(types);
        Collections.sort(sorted, (a, b) ->
                Integer.compare(a.getTickets_price(), b.getTickets_price())
        );

        TicketInfor generalType = sorted.get(0);                       // rẻ nhất => General
        TicketInfor premiumType = sorted.get(sorted.size() - 1);       // đắt nhất => Premium
        TicketInfor vipType     = (sorted.size() > 2) ? sorted.get(1) : null; // giữa => VIP (nếu có)

        long cheapest  = generalType.getTickets_price();
        long expensive = premiumType.getTickets_price();
        long mid       = (vipType != null)
                ? vipType.getTickets_price()
                : cheapest;

        // Layout giả định: A = premium, B = VIP, còn lại = general
        priceStd  = cheapest;   // General
        priceVip  = mid;        // VIP
        priceVvip = expensive;  // Premium

        // ====== LẤY QUANTITY & SOLD TỪ FIRESTORE RỒI TÍNH GHẾ CÒN LẠI ======
        int generalQty = generalType.getTickets_quantity();                 // ví dụ 200
        int premiumQty = premiumType.getTickets_quantity();                // ví dụ 40
        int vipQty     = (vipType != null) ? vipType.getTickets_quantity() : 0; // ví dụ 80

        int generalSold  = generalType.getTickets_sold();
        int premiumSold  = premiumType.getTickets_sold();
        int vipSold      = (vipType != null) ? vipType.getTickets_sold() : 0;

        // Ghế còn lại = quantity - sold (không cho âm)
        generalQty = Math.max(0, generalQty - generalSold);
        premiumQty = Math.max(0, premiumQty - premiumSold);
        vipQty     = Math.max(0, vipQty - vipSold);

        // Gán vào 3 biến seats* dùng cho sơ đồ ghế
        if (generalQty > 0) {
            seatsGeneral = generalQty;
        }
        if (premiumQty > 0) {
            seatsPremium = premiumQty;
        }
        if (vipQty > 0) {
            seatsVip = vipQty;
        }

        // Tránh case cả 3 = 0 (thiết kế sai dữ liệu) -> fallback layout cũ
        if (seatsPremium <= 0 && seatsVip <= 0 && seatsGeneral <= 0) {
            seatsPremium = 6;
            seatsVip     = 6;
            seatsGeneral = 12;
        }

        totalSeatCount = seatsPremium + seatsVip + seatsGeneral;
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
                    Log.d(TAG, "loadReservedSeatsFromFirestore: reservedSeats=" + reservedSeats.size());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "loadReservedSeatsFromFirestore: fail", e);
                    Snackbar.make(requireView(),
                            "Không tải được trạng thái ghế, hiển thị mặc định.",
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    // Dù success hay fail -> tiến hành init seats với reservedSeats hiện có
                    initSeatsLazy();
                    renderSummary();
                });
    }

    // ================== SEATS & UI (LAZY LOAD) ==================

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

    /** Khởi tạo lại state + generate page đầu tiên. */
    private void initSeatsLazy() {
        stateBySeat.clear();
        selected.clear();
        seatItems.clear();
        selectedPremiumCount = 0;
        selectedVipCount     = 0;
        selectedGeneralCount = 0;
        generatedSeatCount   = 0;
        isLoadingMore        = false;

        if (totalSeatCount <= 0) {
            totalSeatCount = seatsPremium + seatsVip + seatsGeneral;
        }

        Log.d(TAG, "initSeatsLazy: totalSeatCount=" + totalSeatCount);

        // 🟢 PRELOAD NHIỀU PAGE ĐẦU TIÊN
        // Ví dụ preload tối đa 3 page đầu (3 * 32 = 96 ghế)
        // để đảm bảo nội dung cao hơn viewport => RecyclerView scroll được.
        int maxInitialPages = 2;
        for (int i = 0; i < maxInitialPages && generatedSeatCount < totalSeatCount; i++) {
            loadNextPage();
        }

        // Nếu em muốn cực chắc thì có thể tăng maxInitialPages lên 4.
    }

    /** Tạo SeatItem tương ứng với index global (0..totalSeatCount-1). */
    @NonNull
    private SeatItem createSeatItemForIndex(int index) {
        char rowLetter;
        int number;

        int premiumEnd = seatsPremium;
        int vipEnd     = seatsPremium + seatsVip;

        if (index < premiumEnd) {
            rowLetter = 'A';
            number    = index + 1;
        } else if (index < vipEnd) {
            rowLetter = 'B';
            number    = index - premiumEnd + 1;
        } else {
            rowLetter = 'C';
            number    = index - vipEnd + 1;
        }

        String seatId = rowLetter + String.valueOf(number);
        SeatCategory category = getCategoryForSeat(seatId);

        SeatState init = reservedSeats.contains(seatId)
                ? SeatState.RESERVED
                : SeatState.AVAILABLE;

        return new SeatItem(seatId, category, init);
    }

    /** Load thêm 1 page ghế (lazy). */
    private void loadNextPage() {
        if (generatedSeatCount >= totalSeatCount) {
            Log.d(TAG, "loadNextPage: no more seats. generated=" + generatedSeatCount
                    + " / total=" + totalSeatCount);
            return;
        }
        if (isLoadingMore) return;

        isLoadingMore = true;

        int startIndex    = generatedSeatCount;
        int endExclusive  = Math.min(startIndex + PAGE_SIZE, totalSeatCount);
        int oldSize       = seatItems.size();

        for (int i = startIndex; i < endExclusive; i++) {
            SeatItem item = createSeatItemForIndex(i);
            seatItems.add(item);
            stateBySeat.put(item.seatId, item.state);
        }

        generatedSeatCount = endExclusive;

        if (seatAdapter != null) {
            seatAdapter.notifyItemRangeInserted(oldSize, seatItems.size() - oldSize);
        }

        Log.d(TAG, "loadNextPage: loaded " + (endExclusive - startIndex)
                + " seats, generated=" + generatedSeatCount + "/" + totalSeatCount);

        isLoadingMore = false;
    }

    /** Label dùng cho text trên ghế. Ở đây chỉ hiển thị mã ghế (A1, B3, ...). */
    @NonNull
    private String buildSeatButtonLabel(@NonNull String seat) {
        return seat;
    }

    private void applySeatStyle(@NonNull MaterialButton btn, @NonNull SeatState st) {
        // Chặn trạng thái toggle mặc định
        btn.setCheckable(false);
        btn.setChecked(false);

        btn.setPressed(false);
        btn.setHovered(false);

        btn.setIcon(null);
        btn.setBackgroundTintList(null);
        btn.setStrokeWidth(0);
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

    private void handleSeatClick(int position) {
        if (position < 0 || position >= seatItems.size()) return;

        SeatItem item = seatItems.get(position);
        String seat   = item.seatId;
        SeatState cur = item.state;

        if (cur == SeatState.RESERVED) return;

        // Chặn click nếu giá chưa load xong
        if (!pricesLoaded) {
            Snackbar.make(requireView(),
                    "Đang tải giá vé, vui lòng chờ một chút...",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        SeatCategory category = item.category;

        if (cur == SeatState.AVAILABLE) {
            // Check quota trước khi cho chọn
            if (!canSelectMore(category)) {
                return;
            }

            item.state = SeatState.SELECTED;
            stateBySeat.put(seat, SeatState.SELECTED);
            selected.add(seat);
            incrementCategoryCounter(category);

        } else if (cur == SeatState.SELECTED) {
            item.state = SeatState.AVAILABLE;
            stateBySeat.put(seat, SeatState.AVAILABLE);
            selected.remove(seat);
            decrementCategoryCounter(category);
        }

        seatAdapter.notifyItemChanged(position);
        renderSummary();
    }

    private long computeTotal() {
        long sum = 0L;
        for (String s : selected) {
            sum += priceOf(s);
        }
        return sum;
    }

    private void updateSelectedText() {
        if (tvSelected == null) return;

        if (selected.isEmpty()) {
            tvSelected.setText("Chưa chọn ghế");
            return;
        }

        StringBuilder sb = new StringBuilder("Ghế: ");
        boolean first = true;
        for (String s : selected) {
            if (!first) sb.append(", ");
            sb.append(s);
            first = false;
        }
        tvSelected.setText(sb.toString());
    }
    // ========== QUOTA HELPERS ==========

    /** Tổng quota nếu màn trước có truyền. */
    private int getTotalQuota() {
        return Math.max(0, quotaPremium)
                + Math.max(0, quotaVip)
                + Math.max(0, quotaGeneral);
    }

    @NonNull
    private SeatCategory getCategoryForSeat(@NonNull String seat) {
        if (seat.isEmpty()) return SeatCategory.GENERAL;
        char row = Character.toUpperCase(seat.charAt(0));
        if (row == 'A') return SeatCategory.PREMIUM;
        if (row == 'B') return SeatCategory.VIP;
        return SeatCategory.GENERAL;
    }

    private void incrementCategoryCounter(@NonNull SeatCategory category) {
        switch (category) {
            case PREMIUM:
                selectedPremiumCount++;
                break;
            case VIP:
                selectedVipCount++;
                break;
            case GENERAL:
                selectedGeneralCount++;
                break;
        }
    }

    private void decrementCategoryCounter(@NonNull SeatCategory category) {
        switch (category) {
            case PREMIUM:
                if (selectedPremiumCount > 0) selectedPremiumCount--;
                break;
            case VIP:
                if (selectedVipCount > 0) selectedVipCount--;
                break;
            case GENERAL:
                if (selectedGeneralCount > 0) selectedGeneralCount--;
                break;
        }
    }

    /**
     * Kiểm tra xem còn được phép chọn thêm ghế thuộc category này không,
     * theo cả tổng quota và quota từng hạng.
     */
    private boolean canSelectMore(@NonNull SeatCategory category) {
        int totalQuota    = getTotalQuota();
        int totalSelected = selectedPremiumCount + selectedVipCount + selectedGeneralCount;

        // 1) Nếu có cấu hình quota tổng -> không cho chọn quá tổng đó
        if (totalQuota > 0 && totalSelected >= totalQuota) {
            Snackbar.make(requireView(),
                    "Bạn đã chọn đủ " + totalQuota + " ghế theo số vé đã mua.",
                    Snackbar.LENGTH_SHORT).show();
            return false;
        }

        // 2) Check quota từng hạng
        switch (category) {
            case PREMIUM:
                if (quotaPremium <= 0) {
                    Snackbar.make(requireView(),
                            "Bạn không mua vé Premium, không thể chọn ghế hàng A.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedPremiumCount >= quotaPremium) {
                    Snackbar.make(requireView(),
                            "Bạn chỉ được chọn tối đa " + quotaPremium + " ghế Premium.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                break;

            case VIP:
                if (quotaVip <= 0) {
                    Snackbar.make(requireView(),
                            "Bạn không mua vé VIP, không thể chọn ghế hàng B.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedVipCount >= quotaVip) {
                    Snackbar.make(requireView(),
                            "Bạn chỉ được chọn tối đa " + quotaVip + " ghế VIP.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                break;

            case GENERAL:
                if (quotaGeneral <= 0) {
                    Snackbar.make(requireView(),
                            "Bạn không mua vé General, không thể chọn ghế hàng C trở đi.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedGeneralCount >= quotaGeneral) {
                    Snackbar.make(requireView(),
                            "Bạn chỉ được chọn tối đa " + quotaGeneral + " ghế General.",
                            Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                break;
        }

        // 3) Nếu không có quota nào được truyền (tổng = 0) -> fallback về MAX_SELECT như logic cũ
        if (totalQuota == 0 && selected.size() >= MAX_SELECT) {
            Snackbar.make(requireView(),
                    "Bạn chỉ có thể chọn tối đa " + MAX_SELECT + " ghế.",
                    Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
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

        // Cập nhật dòng "Ghế: ..."
        updateSelectedText();

        // Cập nhật tổng tiền
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

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        View content = getLayoutInflater().inflate(R.layout.bottomsheet_seat_info, null, false);
        TextView tvTitle = content.findViewById(R.id.tv_seat_info_title);
        TextView tvZone  = content.findViewById(R.id.tv_seat_info_zone);
        TextView tvPrice = content.findViewById(R.id.tv_seat_info_price);
        View btnClose    = content.findViewById(R.id.btn_seat_info_close);

        // Chỉ hiện MÃ GHẾ ở title, ví dụ "A1"
        if (tvTitle != null) {
            tvTitle.setText(seat);
        }
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

    // ================== Adapter ==================

    private class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatVH> {

        @NonNull
        @Override
        public SeatVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_seat, parent, false);
            return new SeatVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SeatVH holder, int position) {
            SeatItem item = seatItems.get(position);
            holder.bind(item, position);
        }

        @Override
        public int getItemCount() {
            return seatItems.size();
        }

        class SeatVH extends RecyclerView.ViewHolder {

            final MaterialButton btnSeat;

            SeatVH(@NonNull View itemView) {
                super(itemView);
                btnSeat = itemView.findViewById(R.id.btn_seat);
            }

            void bind(@NonNull SeatItem item, int position) {
                btnSeat.setText(buildSeatButtonLabel(item.seatId));
                applySeatStyle(btnSeat, item.state);

                btnSeat.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    handleSeatClick(pos);
                });

                btnSeat.setOnLongClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return false;
                    SeatItem current = seatItems.get(pos);
                    showSeatInfoBottomSheet(current.seatId);
                    return true;
                });
            }
        }
    }
}
