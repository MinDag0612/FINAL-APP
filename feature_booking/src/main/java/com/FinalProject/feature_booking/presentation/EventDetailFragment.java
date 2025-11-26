package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Màn chi tiết sự kiện cho flow booking.
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

        // ===== Điều hướng chọn chỗ ngồi: MỞ BOTTOM SHEET CHỌN VÉ =====
        if (btnChooseSeat != null) {
            btnChooseSeat.setOnClickListener(v -> openTicketTypeBottomSheet());
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

    // ============= Bottom sheet chọn loại vé + số lượng =============

    /**
     * Mở bottom sheet chọn loại vé + số lượng.
     * - Load TicketInfor từ Firestore qua BookingRepository.
     * - Map theo giá thành 3 hạng: General (rẻ nhất), VIP (giữa), Premium (đắt nhất).
     */
    private void openTicketTypeBottomSheet() {
        if (TextUtils.isEmpty(eventId)) {
            Snackbar.make(requireView(),
                    "Thiếu thông tin sự kiện, không thể chọn vé.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        bookingRepo.getTicketTypesForEvent(eventId)      // Task<List<TicketInfor>>
                .addOnSuccessListener(infors -> {
                    if (!isAdded()) return;

                    if (infors == null || infors.isEmpty()) {
                        Snackbar.make(requireView(),
                                "Hiện chưa có cấu hình loại vé cho sự kiện này.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    showTicketTypeBottomSheet(infors);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Không tải được danh sách vé: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void showTicketTypeBottomSheet(@NonNull List<TicketInfor> types) {
        // Sắp xếp theo giá: rẻ -> đắt
        List<TicketInfor> sorted = new ArrayList<>(types);
        Collections.sort(sorted, (a, b) ->
                Integer.compare(a.getTickets_price(), b.getTickets_price())
        );

        TicketInfor generalType = sorted.get(0);
        TicketInfor premiumType = sorted.get(sorted.size() - 1);
        TicketInfor vipType     = sorted.size() > 2 ? sorted.get(1) : null; // nếu chỉ có 2 loại thì có thể bỏ VIP

        // ✅ CODE MỚI: dùng quantity - sold
        int maxGeneral = computeRemaining(generalType);
        int maxPremium = computeRemaining(premiumType);
        int maxVip     = computeRemaining(vipType);

        // Dùng constructor đơn giản để tránh lỗi style/R
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        View content = getLayoutInflater()
                .inflate(R.layout.bottomsheet_ticket_type_selection, null, false);

        dialog.setContentView(content);

        // ========== Bind Premium ==========
        TextView tvPricePremium      = content.findViewById(R.id.tv_ticket_price_premium);
        TextView tvRemainPremium     = content.findViewById(R.id.tv_ticket_remaining_premium);
        TextView tvQtyPremium        = content.findViewById(R.id.tv_ticket_qty_premium);
        View btnMinusPremium         = content.findViewById(R.id.btn_ticket_minus_premium);
        View btnPlusPremium          = content.findViewById(R.id.btn_ticket_plus_premium);

        // ========== Bind VIP ==========
        LinearLayout rowVip          = content.findViewById(R.id.row_vip);
        TextView tvPriceVip          = content.findViewById(R.id.tv_ticket_price_vip);
        TextView tvRemainVip         = content.findViewById(R.id.tv_ticket_remaining_vip);
        TextView tvQtyVip            = content.findViewById(R.id.tv_ticket_qty_vip);
        View btnMinusVip             = content.findViewById(R.id.btn_ticket_minus_vip);
        View btnPlusVip              = content.findViewById(R.id.btn_ticket_plus_vip);

        // ========== Bind General ==========
        TextView tvPriceGeneral      = content.findViewById(R.id.tv_ticket_price_general);
        TextView tvRemainGeneral     = content.findViewById(R.id.tv_ticket_remaining_general);
        TextView tvQtyGeneral        = content.findViewById(R.id.tv_ticket_qty_general);
        View btnMinusGeneral         = content.findViewById(R.id.btn_ticket_minus_general);
        View btnPlusGeneral          = content.findViewById(R.id.btn_ticket_plus_general);

        // ========== Giá & quantity hiển thị ==========
        if (premiumType != null && tvPricePremium != null) {
            tvPricePremium.setText(vnd.format(premiumType.getTickets_price()));
        }
        if (tvRemainPremium != null) {
            tvRemainPremium.setText("Còn " + maxPremium + " vé");
        }

        if (vipType == null && rowVip != null) {
            // Nếu không có loại VIP rõ ràng -> ẩn row VIP
            rowVip.setVisibility(View.GONE);
        } else if (vipType != null) {
            if (tvPriceVip != null) {
                tvPriceVip.setText(vnd.format(vipType.getTickets_price()));
            }
            if (tvRemainVip != null) {
                tvRemainVip.setText("Còn " + maxVip + " vé");
            }
        }

        if (generalType != null && tvPriceGeneral != null) {
            tvPriceGeneral.setText(vnd.format(generalType.getTickets_price()));
        }
        if (tvRemainGeneral != null) {
            tvRemainGeneral.setText("Còn " + maxGeneral + " vé");
        }

        // ✅ Áp dụng state "Còn N vé" / "Hết vé" + enable/disable nút
        bindRowState(tvRemainPremium,  btnMinusPremium,  btnPlusPremium,  maxPremium);
        bindRowState(tvRemainVip,      btnMinusVip,      btnPlusVip,      maxVip);
        bindRowState(tvRemainGeneral,  btnMinusGeneral,  btnPlusGeneral,  maxGeneral);

        // ========== Logic tăng/giảm số lượng ==========
        final int[] qtyPremium = {0};
        final int[] qtyVip     = {0};
        final int[] qtyGeneral = {0};

        if (tvQtyPremium != null) tvQtyPremium.setText("0");
        if (tvQtyVip != null)     tvQtyVip.setText("0");
        if (tvQtyGeneral != null) tvQtyGeneral.setText("0");

        if (btnMinusPremium != null) {
            btnMinusPremium.setOnClickListener(v -> {
                if (qtyPremium[0] > 0) {
                    qtyPremium[0]--;
                    tvQtyPremium.setText(String.valueOf(qtyPremium[0]));
                }
            });
        }
        if (btnPlusPremium != null) {
            btnPlusPremium.setOnClickListener(v -> {
                if (qtyPremium[0] < maxPremium) {
                    qtyPremium[0]++;
                    tvQtyPremium.setText(String.valueOf(qtyPremium[0]));
                } else {
                    Snackbar.make(requireView(),
                            "Tối đa " + maxPremium + " vé Premium.",
                            Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMinusVip != null) {
            btnMinusVip.setOnClickListener(v -> {
                if (qtyVip[0] > 0) {
                    qtyVip[0]--;
                    tvQtyVip.setText(String.valueOf(qtyVip[0]));
                }
            });
        }
        if (btnPlusVip != null) {
            btnPlusVip.setOnClickListener(v -> {
                if (maxVip <= 0) return;
                if (qtyVip[0] < maxVip) {
                    qtyVip[0]++;
                    tvQtyVip.setText(String.valueOf(qtyVip[0]));
                } else {
                    Snackbar.make(requireView(),
                            "Tối đa " + maxVip + " vé VIP.",
                            Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMinusGeneral != null) {
            btnMinusGeneral.setOnClickListener(v -> {
                if (qtyGeneral[0] > 0) {
                    qtyGeneral[0]--;
                    tvQtyGeneral.setText(String.valueOf(qtyGeneral[0]));
                }
            });
        }
        if (btnPlusGeneral != null) {
            btnPlusGeneral.setOnClickListener(v -> {
                if (qtyGeneral[0] < maxGeneral) {
                    qtyGeneral[0]++;
                    tvQtyGeneral.setText(String.valueOf(qtyGeneral[0]));
                } else {
                    Snackbar.make(requireView(),
                            "Tối đa " + maxGeneral + " vé General.",
                            Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        // ========== Confirm -> giữ nguyên như cũ ==========
        View btnConfirm = content.findViewById(R.id.btn_confirm_ticket_types);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                int total = qtyPremium[0] + qtyVip[0] + qtyGeneral[0];
                if (total <= 0) {
                    Snackbar.make(requireView(),
                            "Vui lòng chọn ít nhất 1 vé.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                String finalTitle = tvTitle != null
                        ? tvTitle.getText().toString()
                        : eventTitleArg;

                Bundle toSeat = new Bundle();
                toSeat.putString("eventId", eventId);
                toSeat.putString("eventTitle", finalTitle);
                toSeat.putString("showId", showId);

                // QUOTA THEO HẠNG -> TRUYỀN SANG SeatSelectionFragment
                toSeat.putInt("qtyPremium", qtyPremium[0]);
                toSeat.putInt("qtyVip", qtyVip[0]);
                toSeat.putInt("qtyGeneral", qtyGeneral[0]);

                dialog.dismiss();

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_eventDetail_to_seatSelection, toSeat);
            });
        }

        dialog.show();
    }


    // ============= Helpers =============

    // Tính số vé còn lại = quantity - sold (không cho âm)
    private int computeRemaining(@Nullable TicketInfor info) {
        if (info == null) return 0;
        int remaining = info.getTickets_quantity() - info.getTickets_sold();
        return Math.max(0, remaining);
    }

    /**
     * Helper nhỏ: set text "Còn N vé" / "Hết vé" + enable/disable nút +/-.
     */
    private void bindRowState(
            @Nullable TextView tvRemain,
            @Nullable View btnMinus,
            @Nullable View btnPlus,
            int remaining
    ) {
        if (tvRemain != null) {
            if (remaining <= 0) {
                tvRemain.setText("Hết vé");
            } else {
                tvRemain.setText("Còn " + remaining + " vé");
            }
        }
        boolean enabled = remaining > 0;
        if (btnMinus != null) btnMinus.setEnabled(enabled);
        if (btnPlus  != null) btnPlus.setEnabled(enabled);
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
