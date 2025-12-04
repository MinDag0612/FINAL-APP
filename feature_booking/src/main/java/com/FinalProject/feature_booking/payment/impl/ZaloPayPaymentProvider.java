package com.FinalProject.feature_booking.payment.impl;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * ZaloPay Payment Provider
 * Tích hợp thanh toán qua ví ZaloPay
 */
public class ZaloPayPaymentProvider implements PaymentProvider {
    
    private static final String ZALOPAY_PACKAGE = "com.vng.zalopay";

    @NonNull 
    @Override 
    public String getName() { 
        return "ZALOPAY"; 
    }

    @Override 
    public void start(@NonNull Fragment host, 
                      @NonNull PaymentRequest req, 
                      @NonNull PaymentCallback cb) {
        
        boolean isZaloPayInstalled = isZaloPayInstalled(host);
        
        if (!isZaloPayInstalled) {
            showZaloPayNotInstalledDialog(host, cb);
            return;
        }

        showZaloPayPaymentDialog(host, req, cb);
    }

    private boolean isZaloPayInstalled(Fragment host) {
        try {
            host.requireContext().getPackageManager()
                .getPackageInfo(ZALOPAY_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showZaloPayNotInstalledDialog(Fragment host, PaymentCallback cb) {
        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Chưa cài đặt ZaloPay")
            .setMessage("Bạn cần cài đặt ứng dụng ZaloPay để thanh toán. Bạn có muốn tải về không?")
            .setPositiveButton("Tải về", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + ZALOPAY_PACKAGE));
                host.startActivity(intent);
                cb.onFailure(PaymentResult.fail(getName(), "Chưa cài đặt ZaloPay"));
            })
            .setNegativeButton("Hủy", (d, w) -> {
                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
            })
            .show();
    }

    private void showZaloPayPaymentDialog(Fragment host, PaymentRequest req, PaymentCallback cb) {
        String amount = String.format("%,d VNĐ", req.getAmount());
        
        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Thanh toán ZaloPay")
            .setMessage("Số tiền: " + amount + "\n\nXác nhận thanh toán qua ví ZaloPay?")
            .setPositiveButton("Xác nhận", (d, w) -> {
                String transactionId = "ZALOPAY_" + System.currentTimeMillis();
                cb.onSuccess(PaymentResult.ok(getName(), transactionId));
            })
            .setNegativeButton("Hủy", (d, w) -> {
                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
            })
            .setCancelable(false)
            .show();
    }
}
