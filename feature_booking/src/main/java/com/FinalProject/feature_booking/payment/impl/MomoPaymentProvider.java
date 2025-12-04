package com.FinalProject.feature_booking.payment.impl;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * MoMo Payment Provider
 * Tích hợp thanh toán qua ví MoMo
 * 
 * Flow:
 * 1. Tạo deep link đến MoMo app
 * 2. User xác nhận thanh toán trong MoMo
 * 3. Callback trả về kết quả
 */
public class MomoPaymentProvider implements PaymentProvider {
    
    private static final String MOMO_PACKAGE = "com.momo.platform";
    private static final int REQUEST_CODE_MOMO = 1001;

    @NonNull 
    @Override 
    public String getName() { 
        return "MOMO"; 
    }

    @Override 
    public void start(@NonNull Fragment host, 
                      @NonNull PaymentRequest req, 
                      @NonNull PaymentCallback cb) {
        
        // Kiểm tra MoMo app có được cài đặt không
        boolean isMomoInstalled = isMomoInstalled(host);
        
        if (!isMomoInstalled) {
            showMomoNotInstalledDialog(host, cb);
            return;
        }

        // Simulate MoMo payment flow
        showMomoPaymentDialog(host, req, cb);
    }

    private boolean isMomoInstalled(Fragment host) {
        try {
            host.requireContext().getPackageManager()
                .getPackageInfo(MOMO_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showMomoNotInstalledDialog(Fragment host, PaymentCallback cb) {
        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Chưa cài đặt MoMo")
            .setMessage("Bạn cần cài đặt ứng dụng MoMo để thanh toán. Bạn có muốn tải về không?")
            .setPositiveButton("Tải về", (d, w) -> {
                // Open Play Store
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + MOMO_PACKAGE));
                host.startActivity(intent);
                cb.onFailure(PaymentResult.fail(getName(), "Chưa cài đặt MoMo"));
            })
            .setNegativeButton("Hủy", (d, w) -> {
                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
            })
            .show();
    }

    private void showMomoPaymentDialog(Fragment host, PaymentRequest req, PaymentCallback cb) {
        String amount = String.format("%,d VNĐ", req.getAmount());
        
        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Thanh toán MoMo")
            .setMessage("Số tiền: " + amount + "\n\nXác nhận thanh toán qua ví MoMo?")
            .setPositiveButton("Xác nhận", (d, w) -> {
                // Simulate successful payment
                String transactionId = "MOMO_" + System.currentTimeMillis();
                cb.onSuccess(PaymentResult.ok(getName(), transactionId));
            })
            .setNegativeButton("Hủy", (d, w) -> {
                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Tạo deep link đến MoMo app
     * Format: momo://app?action=payment&amount=100000&orderId=xxx
     */
    private String createMomoDeepLink(PaymentRequest req) {
        String orderId = req.eventId + "_" + System.currentTimeMillis();
        String description = "Thanh toan ve su kien " + req.eventId;
        return String.format("momo://app?action=payment&amount=%d&orderId=%s&description=%s",
            req.getAmount(),
            orderId,
            Uri.encode(description)
        );
    }
}
