package com.FinalProject.feature_booking.presentation;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;

// ZXing
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TicketDetailFragment extends Fragment {

    private BookingRepository bookingRepo;

    // Views
    private TextView tvTitle;
    private TextView tvSeats;
    private TextView tvMethod;
    private TextView tvTotal;
    private TextView tvDateTime;
    private TextView tvVenue;
    private TextView tvTicketId;
    private ImageView ivQr;
    private View btnBack;
    private View btnDownload;
    private View btnShare;

    private String orderIdArg;

    private final NumberFormat vnd =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TicketDetailFragment() {
        super(R.layout.fragment_ticket_detail);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        bookingRepo = BookingRepository.getInstance();

        // Bind view
        tvTitle    = v.findViewById(R.id.tv_ticket_event_title);
        tvSeats    = v.findViewById(R.id.tv_ticket_seats);
        tvMethod   = v.findViewById(R.id.tv_ticket_payment_method);
        tvTotal    = v.findViewById(R.id.tv_ticket_total);
        ivQr       = v.findViewById(R.id.iv_ticket_qr);
        tvDateTime = v.findViewById(R.id.tv_ticket_datetime);
        tvVenue    = v.findViewById(R.id.tv_ticket_venue);
        tvTicketId = v.findViewById(R.id.tv_ticket_id);

        btnBack     = v.findViewById(R.id.btn_back_to_home);
        btnDownload = v.findViewById(R.id.btn_download_ticket);
        btnShare    = v.findViewById(R.id.btn_share_ticket);

        // Đọc orderId từ arguments (đi từ MyTicketsFragment)
        Bundle args = getArguments();
        orderIdArg = args != null ? args.getString("orderId", "") : "";

        if (TextUtils.isEmpty(orderIdArg)) {
            if (isAdded()) {
                Snackbar.make(requireView(),
                        "Không tìm thấy orderId cho vé này.",
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            loadOrderFromFirestore(orderIdArg);
        }

        // Nút quay về
        if (btnBack != null) {
            btnBack.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).popBackStack()
            );
        }

        // Nút DOWNLOAD (lưu PNG QR vào cache/share/)
        if (btnDownload != null) {
            btnDownload.setOnClickListener(x -> {
                Bitmap bmp = getBitmapFromImageView(ivQr);
                if (bmp == null) {
                    Snackbar.make(requireView(), "Không tìm thấy ảnh QR để tải.", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Uri savedUri = saveQrToCache(bmp);
                if (savedUri != null) {
                    Snackbar.make(requireView(), "Đã lưu QR vào bộ nhớ tạm (cache/share/).", Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(requireView(), "Không thể lưu QR.", Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        // Nút SHARE (chia sẻ ảnh QR)
        if (btnShare != null) {
            btnShare.setOnClickListener(x -> {
                Bitmap bmp = getBitmapFromImageView(ivQr);
                if (bmp == null) {
                    Snackbar.make(requireView(), "Không tìm thấy ảnh QR để chia sẻ.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                Uri shareUri = saveQrToCache(bmp);
                if (shareUri == null) {
                    Snackbar.make(requireView(), "Không thể chuẩn bị file QR để chia sẻ.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("image/png");
                sendIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                sendIntent.setClipData(ClipData.newRawUri("ticket_qr", shareUri));
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(
                        sendIntent,
                        getString(R.string.ticket_share)
                ));
            });
        }
    }

    // ================== Firestore: load Orders/{orderId} + Events/{event_id} ==================

    private void loadOrderFromFirestore(@NonNull String orderId) {
        bookingRepo.getOrderById(orderId)
                .addOnSuccessListener(orderDoc -> {
                    if (!isAdded()) return;
                    if (orderDoc == null || !orderDoc.exists()) {
                        Snackbar.make(requireView(),
                                "Không tìm thấy đơn vé với ID: " + orderId,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String eventId = orderDoc.getString("event_id");
                    if (TextUtils.isEmpty(eventId)) {
                        // Không có event_id thì bind dựa trên riêng Order
                        bindOrderAndEvent(orderDoc, null);
                        return;
                    }

                    // Join sang Events/{eventId} để lấy thêm thông tin
                    bookingRepo.getEventDocument(eventId)
                            .addOnSuccessListener(eventDoc -> {
                                if (!isAdded()) return;
                                bindOrderAndEvent(orderDoc, eventDoc);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                // Nếu fail event, vẫn bind được phần Order
                                bindOrderAndEvent(orderDoc, null);
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(),
                            "Lỗi khi tải đơn vé: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    @SuppressWarnings("unchecked")
    private void bindOrderAndEvent(@NonNull DocumentSnapshot orderDoc,
                                   @Nullable DocumentSnapshot eventDoc) {

        String orderId       = orderDoc.getId();
        String eventId       = orderDoc.getString("event_id");
        String showId        = orderDoc.getString("show_id");
        Long totalPriceLong  = orderDoc.getLong(StoreField.OrderFields.TOTAL_PRICE);
        Boolean isPaid       = orderDoc.getBoolean(StoreField.OrderFields.IS_PAID);
        String paymentMethod = orderDoc.getString(StoreField.OrderFields.PAYMENT_METHOD);
        String userId        = orderDoc.getString(StoreField.OrderFields.USER_ID);
        String qrCodeStr     = orderDoc.getString("qr_code"); // 🔹 payload QR lưu trong Order

        long totalPrice = (totalPriceLong != null) ? totalPriceLong : 0L;
        boolean paid    = (isPaid != null) ? isPaid : false;

        String payLabel = mapPaymentLabel(paymentMethod, paid);

        List<?> rawItems = (List<?>) orderDoc.get(StoreField.OrderFields.TICKET_ITEMS);
        String ticketSummary = buildTicketSummary(rawItems); // Ví dụ: "VIP x2, STD x1"

        // ----- Thông tin từ Events/{eventId} -----
        String eventTitle = null;
        String dateTime   = null;
        String venue      = null;

        if (eventDoc != null && eventDoc.exists()) {
            eventTitle = safeGetString(eventDoc, StoreField.EventFields.EVENT_NAME);
            venue      = safeGetString(eventDoc, StoreField.EventFields.EVENT_LOCATION);

            // hiển thị: ưu tiên event_datetime, fallback EVENT_DATE, cuối cùng event_start (Seeder)
            dateTime   = safeGetString(eventDoc, "event_datetime");
            if (TextUtils.isEmpty(dateTime)) {
                dateTime = safeGetString(eventDoc, StoreField.EventFields.EVENT_DATE);
            }
            if (TextUtils.isEmpty(dateTime)) {
                // 🔹 hỗ trợ event_start như Seeder (ISO: 2025-12-10T19:00:00Z)
                dateTime = safeGetString(eventDoc, "event_start");
            }
        }

        if (TextUtils.isEmpty(eventTitle)) {
            eventTitle = "Sự kiện";
        }

        // ----- Bind UI -----
        if (tvTitle != null) {
            tvTitle.setText(eventTitle);
        }

        if (tvTicketId != null) {
            tvTicketId.setText("Mã vé: " + orderId);
            tvTicketId.setVisibility(View.VISIBLE);
        }

        if (tvSeats != null) {
            String label = TextUtils.isEmpty(ticketSummary) ? "-" : ticketSummary;
            tvSeats.setText("Vé: " + label);
        }

        if (tvMethod != null) {
            tvMethod.setText("Thanh toán: " + payLabel);
        }

        if (tvTotal != null) {
            tvTotal.setText("Tổng tiền: " + vnd.format(totalPrice));
        }

        if (tvDateTime != null) {
            if (!TextUtils.isEmpty(dateTime)) {
                tvDateTime.setText("Thời gian: " + dateTime);
            } else if (!TextUtils.isEmpty(showId)) {
                tvDateTime.setText("Suất diễn: " + showId);
            }
        }

        if (tvVenue != null && !TextUtils.isEmpty(venue)) {
            tvVenue.setText("Địa điểm: " + venue);
        }

        // ----- showInfo để dùng chung cho QR -----
        StringBuilder showSb = new StringBuilder();
        if (!TextUtils.isEmpty(dateTime)) {
            showSb.append(dateTime);
        }
        if (!TextUtils.isEmpty(venue)) {
            if (showSb.length() > 0) showSb.append(" • ");
            showSb.append(venue);
        }
        if (showSb.length() == 0 && !TextUtils.isEmpty(showId)) {
            showSb.append(showId);
        }
        String showInfo = showSb.toString();

        // ----- QR payload: ưu tiên lấy từ field qr_code trong Order -----
        String payload;
        if (!TextUtils.isEmpty(qrCodeStr)) {
            // 🔹 Flow mới: dùng đúng payload đã lưu, đồng bộ với ScanTicket
            payload = qrCodeStr;
        } else {
            // 🔹 Fallback: build JSON như flow cũ
            payload = "{"
                    + "\"ticketId\":\"" + safe(orderId) + "\","
                    + "\"event\":\""    + safe(eventTitle) + "\","
                    + "\"summary\":\""  + safe(ticketSummary) + "\","
                    + "\"show\":\""     + safe(showInfo) + "\""
                    + "}";
        }

        Bitmap bmp = createQrBitmap(payload, dp(220));
        if (bmp != null && ivQr != null) {
            ivQr.setImageBitmap(bmp);
        }
    }

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

    // ================== QR helpers ==================

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

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    // ================== FileProvider helpers (download/share) ==================

    @Nullable
    private Bitmap getBitmapFromImageView(@Nullable ImageView iv) {
        if (iv == null || iv.getDrawable() == null) return null;
        if (iv.getDrawable() instanceof BitmapDrawable) {
            return ((BitmapDrawable) iv.getDrawable()).getBitmap();
        }
        return null;
    }

    @Nullable
    private Uri saveQrToCache(@NonNull Bitmap bmp) {
        try {
            File cacheDir = new File(requireContext().getCacheDir(), "share");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                return null;
            }
            File outFile = new File(cacheDir, "ticket_qr_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            String authority = requireContext().getPackageName() + ".fileprovider";
            return FileProvider.getUriForFile(requireContext(), authority, outFile);
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    // ================== Helpers ==================

    @Nullable
    private String safeGetString(@NonNull DocumentSnapshot doc, @NonNull String field) {
        try {
            Object v = doc.get(field);
            return v != null ? String.valueOf(v) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private String mapPaymentLabel(@Nullable String paymentMethod, boolean paid) {
        if (TextUtils.isEmpty(paymentMethod)) {
            return paid ? "ĐÃ THANH TOÁN" : "CHƯA THANH TOÁN";
        }
        String upper = paymentMethod.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "CARD":
                return "Thẻ (CARD)";
            case "WALLET":
                return "Ví điện tử";
            case "QR":
                return "QR Banking";
            default:
                return paymentMethod;
        }
    }
}
