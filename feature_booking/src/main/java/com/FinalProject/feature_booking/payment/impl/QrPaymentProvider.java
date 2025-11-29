package com.FinalProject.feature_booking.payment.impl;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

public class QrPaymentProvider implements PaymentProvider {
    @NonNull @Override public String getName() { return "QR"; }

    @Override public void start(@NonNull Fragment host, @NonNull PaymentRequest req, @NonNull PaymentCallback cb) {
        final boolean[] done = { false };

        String payload = "VietQR|bank=970415|acc=123456789|amt=" + req.getAmount()
                + "|content=EVT_" + req.eventId + "_" + req.showId;

        View view = LayoutInflater.from(host.requireContext())
                .inflate(R.layout.dialog_qr_payment, null, false);
        ImageView img = view.findViewById(R.id.img_qr);

        Bitmap bmp = generateQr(payload, 720);
        if (bmp != null) img.setImageBitmap(bmp);

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(host.requireContext())
                        .setTitle("Quét QR ngân hàng")
                        .setView(view)
                        .setPositiveButton("Tôi đã chuyển", (d, w) -> {
                            if (!done[0]) {
                                done[0] = true;
                                cb.onSuccess(PaymentResult.ok(getName(), "QR-" + System.currentTimeMillis()));
                            }
                        })
                        .setNegativeButton("Đóng", (d, w) -> {
                            if (!done[0]) {
                                done[0] = true;
                                cb.onFailure(PaymentResult.fail(getName(), "Người dùng đóng"));
                            }
                        })
                        .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(d -> {
            if (!done[0]) {
                done[0] = true;
                cb.onFailure(PaymentResult.fail(getName(), "Đã đóng hộp thoại"));
            }
        });
        dialog.setOnDismissListener(d -> {
            if (!done[0]) {
                done[0] = true;
                cb.onFailure(PaymentResult.fail(getName(), "Đã đóng hộp thoại"));
            }
        });
        dialog.show();
    }

    private Bitmap generateQr(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix m = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            return bmp;
        } catch (WriterException e) { return null; }
    }
}
