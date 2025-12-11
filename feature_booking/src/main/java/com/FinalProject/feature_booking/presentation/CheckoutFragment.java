package com.FinalProject.feature_booking.presentation;

import com.FinalProject.feature_booking.R;

import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Promotion_API;
import com.FinalProject.feature_booking.payment.PaymentCallback;
import com.FinalProject.feature_booking.payment.PaymentMethod;
import com.FinalProject.feature_booking.payment.PaymentOrchestrator;
import com.FinalProject.feature_booking.payment.PaymentRequest;
import com.FinalProject.feature_booking.payment.PaymentResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;   // 🔹 Dùng cho sort TicketType
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutFragment extends Fragment {

    private BookingRepository bookingRepo;

    private static final double SERVICE_FEE_RATE = 0.03;
    private static final long   SERVICE_FEE_MIN  = 3000;

    private TextView tvEventTitle, tvSeats, tvQuantity, tvTicketPrice, tvServiceFee, tvTotal;

    // Dòng "Giảm giá"
    private View rowDiscount;           // @id/ll_row_discount
    private TextView tvDiscount;        // @id/tv_checkout_discount

    // 4 nút phương thức chính (single-select)
    private MaterialButton btnCard, btnWallet, btnQr, btnBank;
    private List<MaterialButton> paymentButtons;
    
    // Lưu e-wallet đang chọn (MoMo, VNPay, ZaloPay)
    private String selectedEWallet = "MoMo"; // Mặc định

    private MaterialButton btnConfirm;

    private String eventId;
    private String showId;
    private String eventTitleArg = "Sự kiện";
    private ArrayList<String> seats = new ArrayList<>();
    private long ticketPrice = 0L;

    private final NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final PaymentOrchestrator payments = new PaymentOrchestrator();

    // Trạng thái
    private boolean isProcessing = false;
    private boolean paymentLocked = false;
    private PaymentMethod selectedMethod = PaymentMethod.CARD;
    private boolean isAuthorized = false;
    private PaymentMethod authorizedMethod = null;
    private PaymentResult lastPaymentResult = null;

    // Promo
    private TextInputLayout tilPromo;           // @id/til_promo (nếu có)
    private TextInputEditText etPromo;          // @id/et_promo (nếu có)
    private String appliedPromoCode = "";       // ✅ CHỈ set khi bấm icon check & hợp lệ
    private String appliedPromotionId = "";     // Firestore promotion ID
    private int appliedDiscountAmount = 0;      // Discount amount từ API

    // Dialog processing
    private AlertDialog processingDialog;
    private View processingView;
    private ProgressBar progressBar;
    private View ivTick;
    private TextView tvProcessingMsg;
    private final Handler ui = new Handler(Looper.getMainLooper());

    public CheckoutFragment() {
        super(R.layout.fragment_checkout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepo = BookingRepository.getInstance();

        tvEventTitle   = view.findViewById(R.id.tv_checkout_event_title);
        tvSeats        = view.findViewById(R.id.tv_checkout_seats);
        tvQuantity     = view.findViewById(R.id.tv_checkout_quantity);
        tvTicketPrice  = view.findViewById(R.id.tv_checkout_ticket_price);
        tvServiceFee   = view.findViewById(R.id.tv_checkout_service_fee);
        tvTotal        = view.findViewById(R.id.tv_checkout_total);

        rowDiscount    = view.findViewById(R.id.ll_row_discount);
        tvDiscount     = view.findViewById(R.id.tv_checkout_discount);

        btnCard   = view.findViewById(R.id.btn_payment_card);
        btnWallet = view.findViewById(R.id.btn_payment_wallet);
        btnQr     = view.findViewById(R.id.btn_payment_qr);
        btnBank   = view.findViewById(R.id.btn_payment_bank);
        paymentButtons = Arrays.asList(btnCard, btnWallet, btnQr, btnBank);
        for (MaterialButton b : paymentButtons) if (b != null) b.setCheckable(true);

        btnConfirm = view.findViewById(R.id.btn_confirm_payment);
        
        MaterialButton btnBack = view.findViewById(R.id.btn_back_checkout);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        }

        // Promo field
        tilPromo = view.findViewById(R.id.til_promo);
        if (tilPromo == null) tilPromo = findFirstTextInputLayout((ViewGroup) view);
        if (tilPromo != null) {
            etPromo = view.findViewById(R.id.et_promo);
            if (etPromo == null) etPromo = (TextInputEditText) tilPromo.getEditText();
            setupPromoField(); // chỉ áp dụng khi bấm icon ✅
        }

        // Args
        Bundle args = getArguments();
        if (args != null) {
            eventId       = args.getString("eventId", "");
            showId        = args.getString("showId", "");
            eventTitleArg = args.getString("eventTitle", "Sự kiện");
            String[] arr  = args.getStringArray("selectedSeats");
            if (arr != null) seats = new ArrayList<>(Arrays.asList(arr));
            ticketPrice   = args.getLong("totalPrice", 0L);
        }

        // Kiểm tra eventId bắt buộc
        if (TextUtils.isEmpty(eventId)) {
            Snackbar.make(view, 
                "Lỗi: Không có thông tin sự kiện.", 
                Snackbar.LENGTH_LONG).show();
            requireActivity().finish();
            return;
        }

        // UI ban đầu
        tvEventTitle.setText(eventTitleArg);
        tvSeats.setText("Ghế: " + (seats.isEmpty() ? "-" : String.join(", ", seats)));
        tvQuantity.setText("Số lượng vé: " + seats.size());

        setSelection(PaymentMethod.CARD);

        // Chọn phương thức ⇒ authorize
        View.OnClickListener choose = v -> {
            if (isProcessing) return;
            if (paymentLocked) {
                Snackbar.make(requireView(),
                        "Thanh toán đã được xác nhận. Không thể thay đổi phương thức.",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            PaymentMethod tapped = idToMethod(v.getId());
            if (tapped == null) return;
            setSelection(tapped);
            startAuthorize(tapped);
        };
        btnCard.setOnClickListener(choose);
        btnQr.setOnClickListener(choose);
        btnWallet.setOnClickListener(v -> {
            if (isProcessing) return;
            if (paymentLocked) {
                Snackbar.make(requireView(),
                        "Thanh toán đã được xác nhận. Không thể thay đổi phương thức.",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            showEWalletDialog();
        });
        btnBank.setOnClickListener(choose);

        // Nhận vé
        btnConfirm.setEnabled(false);
        btnConfirm.setOnClickListener(v -> {
            if (isProcessing) return;
            if (!isAuthorized || authorizedMethod == null) {
                Snackbar.make(requireView(),
                        "Vui lòng xác nhận phương thức thanh toán trước.",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuthHelper.getCurrentUserUid();
            if (TextUtils.isEmpty(userId)) {
                Snackbar.make(requireView(),
                        "Bạn cần đăng nhập để nhận vé.",
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            createOrderAfterPayment(userId);
        });
    }

    // ---------------- Promo: chỉ áp dụng khi bấm icon ✅ ----------------
    private void setupPromoField() {
        if (tilPromo == null) return;

        tilPromo.setError(null);

        try {
            tilPromo.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            tilPromo.setEndIconDrawable(R.drawable.ic_check_24);
            tilPromo.setEndIconContentDescription("Áp dụng mã");
            tilPromo.setEndIconOnClickListener(v -> {
                String code = getPromoText();
                validatePromoCodeWithAPI(code);
            });
        } catch (Exception ignore) {}

        if (etPromo != null) {
            etPromo.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    // Gõ lại ⇒ hủy mã đã áp dụng (yêu cầu nhấn ✓ lại)
                    if (!appliedPromoCode.isEmpty()) {
                        appliedPromoCode = "";
                        appliedPromotionId = "";
                        appliedDiscountAmount = 0;
                    }
                    tilPromo.setError(null);
                    tilPromo.setHelperText(null);
                    recalcTotalsFor(selectedMethod); // tính lại KHÔNG giảm giá
                }
            });
        }
    }

    // ---------------- Validate Promo với Promotion_API ----------------
    private void validatePromoCodeWithAPI(@NonNull String code) {
        if (code.isEmpty()) {
            tilPromo.setError("Vui lòng nhập mã khuyến mãi");
            return;
        }

        String userId = FirebaseAuthHelper.getCurrentUserUid();
        if (userId == null) {
            tilPromo.setError("Bạn cần đăng nhập để sử dụng mã khuyến mãi");
            return;
        }

        long orderAmount = ticketPrice;
        int ticketCount = seats.size();

        tilPromo.setEnabled(false);
        tilPromo.setHelperText("Đang kiểm tra mã...");

        Promotion_API.validatePromotion(code, userId, eventId, (int) orderAmount, ticketCount)
                .addOnSuccessListener(result -> {
                    if (!isAdded()) return;

                    boolean isValid = (boolean) result.get("isValid");
                    
                    if (isValid) {
                        appliedPromoCode = code.toUpperCase(Locale.ROOT);
                        appliedPromotionId = (String) result.get("promotion_id");
                        
                        Integer discount = (Integer) result.get("discount_amount");
                        appliedDiscountAmount = (discount != null) ? discount : 0;
                        
                        tilPromo.setError(null);
                        tilPromo.setHelperText("Đã áp dụng: " + appliedPromoCode + " (-" + 
                                NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                                        .format(appliedDiscountAmount) + ")");
                    } else {
                        appliedPromoCode = "";
                        appliedPromotionId = "";
                        appliedDiscountAmount = 0;
                        
                        String message = (String) result.get("message");
                        tilPromo.setHelperText(null);
                        tilPromo.setError(message != null ? message : "Mã không hợp lệ");
                    }
                    
                    tilPromo.setEnabled(true);
                    hideKeyboard();
                    recalcTotalsFor(selectedMethod);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    
                    appliedPromoCode = "";
                    appliedPromotionId = "";
                    appliedDiscountAmount = 0;
                    
                    tilPromo.setEnabled(true);
                    tilPromo.setHelperText(null);
                    tilPromo.setError("Lỗi kiểm tra mã: " + 
                            (e != null ? e.getMessage() : "Không rõ"));
                    
                    hideKeyboard();
                    recalcTotalsFor(selectedMethod);
                });
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            View v = getView();
            if (imm != null && v != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception ignore) {}
    }

    // ---------------- E-Wallet Selection Dialog ----------------
    private void showEWalletDialog() {
        String[] wallets = {"MoMo", "VNPay", "ZaloPay"};
        int currentSelection = 0;
        
        // Tìm vị trí hiện tại
        for (int i = 0; i < wallets.length; i++) {
            if (wallets[i].equals(selectedEWallet)) {
                currentSelection = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn ví điện tử")
                .setSingleChoiceItems(wallets, currentSelection, (dialog, which) -> {
                    selectedEWallet = wallets[which];
                })
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    // Cập nhật text button
                    btnWallet.setText("Ví điện tử (" + selectedEWallet + ")");
                    
                    // Chọn và authorize WALLET method
                    setSelection(PaymentMethod.WALLET);
                    startAuthorize(PaymentMethod.WALLET);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }



    @NonNull
    private String getPromoText() {
        if (tilPromo != null && tilPromo.getEditText() != null) {
            return String.valueOf(tilPromo.getEditText().getText()).trim();
        }
        return "";
    }

    // ---------------- Bundle vé ----------------
    private long parseCurrencyTextView(TextView tv) {
        try {
            String s = tv.getText().toString().replaceAll("[^\\d]", "");
            return Long.parseLong(s);
        } catch (Exception e) { return 0L; }
    }

    private Bundle buildTicketBundle(@NonNull String orderId, long totalPaid) {
        Bundle b = new Bundle();
        // ticketId = orderId Firestore → dùng cho QR/chi tiết/scan
        b.putString("ticketId", orderId);
        b.putString("eventTitle", eventTitleArg);
        b.putString("showId", showId);
        b.putStringArray("seats", seats.toArray(new String[0]));
        b.putLong("totalPaid", totalPaid);

        String methodLabel = (authorizedMethod != null)
                ? paymentMethodLabel(authorizedMethod)
                : "ĐÃ THANH TOÁN";
        b.putString("paymentMethod", methodLabel);

        return b;
    }

    @NonNull
    private String paymentMethodLabel(@NonNull PaymentMethod m) {
        switch (m) {
            case CARD:
                return "Thẻ ngân hàng";
            case WALLET:
                return "Ví điện tử (" + selectedEWallet + ")";
            case BANK_TRANSFER:
                return "Chuyển khoản";
            default:
                return "ĐÃ THANH TOÁN";
        }
    }

    // ---------------- Summary & QR payload ----------------

    /** Build text summary kiểu "VIP x2, STD x1" dựa trên qtyByType (typeId = tickets_class). */
    @NonNull
    private String buildTicketSummaryFromQty(@NonNull Map<String, Integer> qtyByType) {
        if (qtyByType.isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : qtyByType.entrySet()) {
            String typeId = e.getKey();
            Integer q = e.getValue();
            if (typeId == null || q == null || q <= 0) continue;
            parts.add(typeId + " x" + q);
        }
        return String.join(", ", parts);
    }

    /**
     * Build payload QR (string) để lưu xuống field qr_code.
     */
    @NonNull
    private String buildQrPayload(@NonNull String orderId,
                                  long totalPaid,
                                  @NonNull Map<String, Integer> qtyByType) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("ticketId", orderId);
            obj.put("event", eventTitleArg != null ? eventTitleArg : "");
            obj.put("summary", buildTicketSummaryFromQty(qtyByType));
            obj.put("show", showId != null ? showId : "");
            obj.put("eventId", eventId != null ? eventId : "");
            obj.put("totalPaid", totalPaid);
            obj.put("paymentMethod", paymentMethodCode(authorizedMethod));

            // Optional: list ghế
            JSONArray arr = new JSONArray();
            for (String s : seats) {
                if (s != null) arr.put(s);
            }
            obj.put("seats", arr);

            return obj.toString();
        } catch (Exception e) {
            // fallback an toàn: ít nhất mã QR chứa được orderId
            return orderId;
        }
    }

    @NonNull
    private String paymentMethodCode(@Nullable PaymentMethod m) {
        if (m == null) return "UNKNOWN";
        switch (m) {
            case CARD:   return "CARD";
            case WALLET: return selectedEWallet.toUpperCase(); // "MOMO", "VNPAY", "ZALOPAY"
            case BANK_TRANSFER: return "BANK_TRANSFER";
            default:     return "UNKNOWN";
        }
    }

    // ---------------- Authorize ----------------
    private void startAuthorize(@NonNull PaymentMethod method) {
        if (seats.isEmpty() || ticketPrice <= 0L) {
            Snackbar.make(requireView(), "Giỏ vé không hợp lệ.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (isProcessing) return;

        // Dùng appliedDiscountAmount từ Promotion_API
        long base      = ticketPrice;
        long discount  = appliedDiscountAmount;
        long subTotal  = Math.max(0L, base - discount);
        long fee       = calcServiceFeeByMethod(subTotal, method);
        long grandTotal= subTotal + fee;

        // Đồng bộ UI trước khi gọi provider
        updatePriceUi(base, discount, fee, grandTotal);

        setUiEnabled(false);
        isProcessing = true;

        PaymentRequest req = new PaymentRequest(
                eventId, showId, seats, grandTotal, "VND", appliedPromoCode, null
        );

        payments.pay(this, method, req, new PaymentCallback() {
            @Override
            public void onSuccess(PaymentResult result) {
                showProcessingDialog("Đang xác minh thanh toán...");
                ui.postDelayed(() -> showProcessingTickThenDismiss(() -> {
                    isProcessing = false;

                    isAuthorized = true;
                    authorizedMethod = method;
                    lastPaymentResult = result;
                    paymentLocked = true;

                    lockPaymentMethodButtons();
                    lockPromoField();
                    btnConfirm.setEnabled(true);

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Xác nhận thành công")
                            .setMessage("Đã xác nhận thanh toán qua " + result.getProvider() +
                                    "\nMã giao dịch: " + result.getTransactionId() +
                                    "\n\nBạn có thể bấm “Nhận vé” để tạo vé của mình.")
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                }), 600);
            }

            @Override
            public void onFailure(PaymentResult result) {
                isProcessing = false;
                paymentLocked = false;

                setUiEnabled(true);
                unlockPromoField();

                isAuthorized = false;
                authorizedMethod = null;
                lastPaymentResult = null;
                btnConfirm.setEnabled(false);

                Snackbar.make(requireView(),
                        "Xác nhận thất bại: " +
                                (result.getMessage() == null ? "Không rõ lỗi" : result.getMessage()),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ---------------- Tạo Order sau khi đã authorize ----------------
    private void createOrderAfterPayment(@NonNull String userId) {
        if (bookingRepo == null) return;
        if (seats == null || seats.isEmpty()) {
            Snackbar.make(requireView(),
                    "Không có ghế nào được chọn.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            Snackbar.make(requireView(),
                    "Thiếu thông tin sự kiện.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        if (showId == null) showId = "";

        final long totalPaid = parseCurrencyTextView(tvTotal);

        setUiEnabled(false);
        btnConfirm.setEnabled(false);
        isProcessing = true;

        bookingRepo.getTicketTypesForEvent(eventId)
                .addOnSuccessListener(types -> {
                    if (!isAdded()) return;

                    if (types == null || types.isEmpty()) {
                        isProcessing = false;
                        setUiEnabled(true);
                        btnConfirm.setEnabled(true);
                        Snackbar.make(requireView(),
                                "Không tải được loại vé cho sự kiện.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    final Map<String, Integer> qtyByType =
                            buildQtyByTypeFromSeats(seats, types);

                    if (qtyByType.isEmpty()) {
                        isProcessing = false;
                        setUiEnabled(true);
                        btnConfirm.setEnabled(true);
                        Snackbar.make(requireView(),
                                "Không xác định được loại vé từ ghế đã chọn.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String paymentMethod = paymentMethodCode(authorizedMethod);

                    // 🔹 DÙNG API MỚI – TRUYỀN LUÔN DANH SÁCH GHẾ XUỐNG ORDER
                    android.util.Log.d("CheckoutFragment", "=== CREATE ORDER START ===");
                    android.util.Log.d("CheckoutFragment", "UserId: " + userId);
                    android.util.Log.d("CheckoutFragment", "EventId: " + eventId);
                    android.util.Log.d("CheckoutFragment", "ShowId: " + showId);
                    
                    bookingRepo.createOrder(userId, eventId, showId, qtyByType, paymentMethod, seats)
                            .addOnSuccessListener(orderId -> {
                                if (!isAdded()) return;
                                isProcessing = false;

                                android.util.Log.d("CheckoutFragment", "Order created! OrderId: " + orderId);

                                if (orderId == null || orderId.trim().isEmpty()) {
                                    setUiEnabled(true);
                                    btnConfirm.setEnabled(true);
                                    Snackbar.make(requireView(),
                                            "Tạo đơn vé thành công nhưng không lấy được ID.",
                                            Snackbar.LENGTH_LONG).show();
                                    return;
                                }

                                // 🔹 Build payload QR và update field qr_code
                                String qrPayload = buildQrPayload(orderId, totalPaid, qtyByType);

                                // 🔹 STEP 1: Update QR Code
                                bookingRepo.updateOrderQrCode(orderId, qrPayload)
                                        .continueWithTask(qrTask -> {
                                            if (!qrTask.isSuccessful()) {
                                                Log.w("CheckoutFragment", "Failed to update QR: " + 
                                                        (qrTask.getException() != null ? qrTask.getException().getMessage() : ""));
                                            }

                                            // 🔹 STEP 2: Update Payment Transaction (nếu có)
                                            if (lastPaymentResult != null && 
                                                lastPaymentResult.getTransactionId() != null) {
                                                return bookingRepo.updatePaymentTransaction(
                                                    orderId,
                                                    lastPaymentResult.getTransactionId(),
                                                    System.currentTimeMillis()
                                                );
                                            }
                                            return Tasks.forResult(null);
                                        })
                                        .continueWithTask(paymentTask -> {
                                            if (paymentTask.getException() != null) {
                                                Log.w("CheckoutFragment", "Failed to update payment transaction: " + 
                                                        paymentTask.getException().getMessage());
                                            }

                                            // 🔹 STEP 3: Update Promotion Info (nếu có)
                                            if (!appliedPromotionId.isEmpty() && appliedDiscountAmount > 0) {
                                                return bookingRepo.updatePromotionInfo(
                                                    orderId,
                                                    appliedPromotionId,
                                                    appliedPromoCode,
                                                    appliedDiscountAmount,
                                                    (int) ticketPrice
                                                );
                                            }
                                            return Tasks.forResult(null);
                                        })
                                        .continueWithTask(promoTask -> {
                                            if (promoTask.getException() != null) {
                                                Log.w("CheckoutFragment", "Failed to update promotion: " + 
                                                        promoTask.getException().getMessage());
                                            }

                                            // 🔹 STEP 4: Apply promotion usage (increment count)
                                            if (!appliedPromotionId.isEmpty()) {
                                                return Promotion_API.applyPromotion(appliedPromotionId, userId, orderId);
                                            }
                                            return Tasks.forResult(null);
                                        })
                                        .addOnCompleteListener(allTask -> {
                                            if (!isAdded()) return;

                                            android.util.Log.d("CheckoutFragment", "Order created successfully. OrderId: " + orderId);
                                            
                                            // Delay nhỏ để Firestore sync data trước khi navigate
                                            requireView().postDelayed(() -> {
                                                if (!isAdded()) return;
                                                
                                                // Navigate to MyTickets
                                                Bundle bundle = buildTicketBundle(orderId, totalPaid);
                                                NavController navController =
                                                        NavHostFragment.findNavController(CheckoutFragment.this);
                                                navController.navigate(R.id.action_checkout_to_myTickets, bundle);
                                            }, 300); // Delay 300ms để Firestore sync
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                isProcessing = false;
                                setUiEnabled(true);
                                btnConfirm.setEnabled(true);
                                Snackbar.make(requireView(),
                                        "Không thể tạo đơn vé: " +
                                                (e != null ? e.getMessage() : ""),
                                        Snackbar.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    isProcessing = false;
                    setUiEnabled(true);
                    btnConfirm.setEnabled(true);
                    Snackbar.make(requireView(),
                            "Không tải được dữ liệu vé: " +
                                    (e != null ? e.getMessage() : ""),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // ---------------- Map ghế → TicketType (theo giá Firestore) ----------------
    /**
     * Map danh sách ghế -> qtyByType (key = TicketType.typeId = tickets_class).
     *
     * Quy ước zone (tương tự SeatSelectionFragment):
     *  - Hàng A: loại ĐẮT NHẤT  (premium)
     *  - Hàng B: loại TRUNG BÌNH (VIP)
     *  - Hàng khác: loại RẺ NHẤT (general)
     */
    /**
     * Map danh sách ghế -> qtyByType (key = tickets_class trong TicketInfor).
     *
     * Quy ước zone (tương tự SeatSelectionFragment):
     *  - Hàng A: loại ĐẮT NHẤT  (Premium)
     *  - Hàng B: loại TRUNG BÌNH (VIP)
     *  - Hàng khác: loại RẺ NHẤT (General/STD)
     */
    private Map<String, Integer> buildQtyByTypeFromSeats(
            @NonNull List<String> seatList,
            @NonNull List<TicketInfor> infos
    ) {
        Map<String, Integer> qty = new HashMap<>();
        if (seatList.isEmpty() || infos.isEmpty()) return qty;

        // Sort TicketInfor theo tickets_price: rẻ -> đắt
        List<TicketInfor> sorted = new ArrayList<>(infos);
        Collections.sort(sorted, (a, b) ->
                Integer.compare(a.getTickets_price(), b.getTickets_price())
        );

        TicketInfor cheapest  = sorted.get(0);
        TicketInfor mid       = (sorted.size() > 1) ? sorted.get(1) : cheapest;
        TicketInfor expensive = sorted.get(sorted.size() - 1);

        String idStd  = cheapest != null  ? cheapest.getTickets_class()  : null; // cho hàng khác A/B
        String idVip  = mid != null       ? mid.getTickets_class()       : null; // cho hàng B
        String idVvip = expensive != null ? expensive.getTickets_class() : null; // cho hàng A

        // Fallback nếu có id null
        if (idStd == null)  idStd  = (idVip != null) ? idVip : idVvip;
        if (idVip == null)  idVip  = (idVvip != null) ? idVvip : idStd;
        if (idVvip == null) idVvip = (idVip != null) ? idVip : idStd;

        for (String seat : seatList) {
            if (seat == null || seat.isEmpty()) continue;
            char row = Character.toUpperCase(seat.charAt(0));

            String typeId;
            if (row == 'A') {
                typeId = idVvip;
            } else if (row == 'B') {
                typeId = idVip;
            } else {
                typeId = idStd;
            }

            if (typeId == null) continue;

            Integer cur = qty.get(typeId);
            qty.put(typeId, (cur == null ? 1 : cur + 1));
        }

        return qty;
    }

    // ---------------- Dialog processing ----------------
    private void showProcessingDialog(@NonNull String message) {
        if (!isAdded()) return;

        if (processingView == null) {
            processingView = getLayoutInflater().inflate(R.layout.dialog_processing, null, false);
            progressBar = processingView.findViewById(R.id.progress);
            ivTick      = processingView.findViewById(R.id.iv_tick);
            tvProcessingMsg = processingView.findViewById(R.id.tv_processing_msg);
        }
        progressBar.setVisibility(View.VISIBLE);
        ivTick.setVisibility(View.GONE);
        tvProcessingMsg.setText(message);

        if (processingDialog == null) {
            processingDialog = new AlertDialog.Builder(requireContext())
                    .setView(processingView)
                    .setCancelable(false)
                    .create();
            processingDialog.setCanceledOnTouchOutside(false);
        }
        if (!processingDialog.isShowing()) processingDialog.show();
    }

    private void showProcessingTickThenDismiss(@NonNull Runnable after) {
        if (!isAdded() || processingView == null) { after.run(); return; }

        progressBar.setVisibility(View.GONE);
        ivTick.setScaleX(0.6f);
        ivTick.setScaleY(0.6f);
        ivTick.setAlpha(0f);
        ivTick.setVisibility(View.VISIBLE);
        tvProcessingMsg.setText("Đã xác minh thành công");

        ivTick.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(220)
                .withEndAction(() ->
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            dismissProcessingDialog();
                            after.run();
                        }, 1200))
                .start();
    }

    private void dismissProcessingDialog() {
        if (processingDialog != null && processingDialog.isShowing()) {
            try { processingDialog.dismiss(); } catch (Exception ignore) {}
        }
    }

    @Override
    public void onDestroyView() {
        dismissProcessingDialog();
        processingDialog = null;
        processingView = null;
        progressBar = null;
        ivTick = null;
        tvProcessingMsg = null;
        super.onDestroyView();
    }

    // ---------------- UI & tính phí ----------------
    private void setSelection(@NonNull PaymentMethod method) {
        selectedMethod = method;
        setChecked(btnCard,   method == PaymentMethod.CARD);
        setChecked(btnWallet, method == PaymentMethod.WALLET);
        setChecked(btnQr,     method == PaymentMethod.QR);
        setChecked(btnBank,   method == PaymentMethod.BANK_TRANSFER);
        
        recalcTotalsFor(method);
    }

    private void recalcTotalsFor(@NonNull PaymentMethod method) {
        long base     = ticketPrice;
        long discount = appliedDiscountAmount; // Dùng discount từ Promotion_API
        long subTotal = Math.max(0L, base - discount);
        long fee      = calcServiceFeeByMethod(subTotal, method);
        long total    = subTotal + fee;

        updatePriceUi(base, discount, fee, total);
    }

    private void updatePriceUi(long base, long discount, long fee, long total) {
        tvTicketPrice.setText(vnd.format(base));

        if (rowDiscount != null) {
            if (discount > 0) {
                rowDiscount.setVisibility(View.VISIBLE);
                if (tvDiscount != null) tvDiscount.setText("-" + vnd.format(discount));
            } else {
                rowDiscount.setVisibility(View.GONE);
            }
        }

        tvServiceFee.setText(vnd.format(fee));
        tvTotal.setText(vnd.format(total));
    }

    private void setUiEnabled(boolean enabled) {
        boolean methodsEnabled = enabled && !isProcessing && !paymentLocked;
        if (paymentButtons != null)
            for (MaterialButton b : paymentButtons)
                if (b != null) b.setEnabled(methodsEnabled);
        if (btnConfirm != null) btnConfirm.setEnabled(enabled && isAuthorized);

        if (tilPromo != null && etPromo != null) {
            boolean promoEnabled = enabled && !isProcessing && !paymentLocked;
            tilPromo.setEnabled(promoEnabled);
            etPromo.setEnabled(promoEnabled);
        }
    }

    private void lockPaymentMethodButtons() {
        if (paymentButtons != null) {
            for (MaterialButton b : paymentButtons) if (b != null) b.setEnabled(false);
        }
    }

    private void lockPromoField() {
        if (tilPromo != null) tilPromo.setEnabled(false);
        if (etPromo != null)  etPromo.setEnabled(false);
    }

    private void unlockPromoField() {
        if (tilPromo != null) tilPromo.setEnabled(true);
        if (etPromo != null)  etPromo.setEnabled(true);
    }

    private long calcServiceFee(long base) {
        if (base <= 0) return 0L;
        long fee = Math.round(base * SERVICE_FEE_RATE);
        return Math.max(fee, SERVICE_FEE_MIN);
    }

    private long calcServiceFeeByMethod(long base, @NonNull PaymentMethod m) {
        long fee = calcServiceFee(base);
        if (m == PaymentMethod.QR) fee = Math.round(fee * 0.7);
        return Math.max(0L, fee);
    }



    @Nullable
    private PaymentMethod idToMethod(@IdRes int id) {
        if (id == R.id.btn_payment_card)   return PaymentMethod.CARD;
        if (id == R.id.btn_payment_wallet) return PaymentMethod.WALLET;
        if (id == R.id.btn_payment_qr)     return PaymentMethod.QR;
        if (id == R.id.btn_payment_bank)   return PaymentMethod.BANK_TRANSFER;
        return null;
    }

    private void setChecked(@Nullable MaterialButton b, boolean checked) {
        if (b != null) b.setChecked(checked);
    }

    @Nullable
    private TextInputLayout findFirstTextInputLayout(@Nullable ViewGroup root) {
        if (root == null) return null;
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c instanceof TextInputLayout) return (TextInputLayout) c;
            if (c instanceof ViewGroup) {
                TextInputLayout found = findFirstTextInputLayout((ViewGroup) c);
                if (found != null) return found;
            }
        }
        return null;
    }
}
