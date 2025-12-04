package com.FinalProject.feature_booking.payment.impl;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Bank Transfer Payment Provider  
 * Thanh toán chuyển khoản ngân hàng
 */
public class BankTransferPaymentProvider implements PaymentProvider {

    @NonNull 
    @Override 
    public String getName() { 
        return "BANK_TRANSFER"; 
    }

    @Override 
    public void start(@NonNull Fragment host, 
                      @NonNull PaymentRequest req, 
                      @NonNull PaymentCallback cb) {
        
        showBankSelectionDialog(host, req, cb);
    }

    private void showBankSelectionDialog(Fragment host, PaymentRequest req, PaymentCallback cb) {
        final String[] banks = {
            "Vietcombank",
            "Techcombank", 
            "BIDV",
            "VietinBank",
            "MB Bank",
            "ACB",
            "Agribank",
            "Sacombank"
        };

        final boolean[] done = { false };

        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Chọn ngân hàng")
            .setItems(banks, (d, which) -> {
                if (!done[0]) {
                    done[0] = true;
                    String selectedBank = banks[which];
                    showBankTransferInfo(host, req, selectedBank, cb);
                }
            })
            .setNegativeButton("Hủy", (d, w) -> {
                if (!done[0]) {
                    done[0] = true;
                    cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
                }
            })
            .setCancelable(false)
            .show();
    }

    private void showBankTransferInfo(Fragment host, PaymentRequest req, 
                                      String bankName, PaymentCallback cb) {
        String amount = String.format("%,d VNĐ", req.getAmount());
        String accountNumber = "1234567890"; // Demo account
        String accountName = "CONG TY SU KIEN ABC";
        String orderId = req.eventId + "_" + System.currentTimeMillis();
        String content = "TT " + orderId;

        String message = String.format(
            "Thông tin chuyển khoản:\n\n" +
            "Ngân hàng: %s\n" +
            "Số tài khoản: %s\n" +
            "Tên tài khoản: %s\n" +
            "Số tiền: %s\n" +
            "Nội dung: %s\n\n" +
            "Vui lòng chuyển khoản và nhập mã giao dịch để xác nhận.",
            bankName, accountNumber, accountName, amount, content
        );

        // Create input field for transaction code
        LinearLayout layout = new LinearLayout(host.requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText input = new EditText(host.requireContext());
        input.setHint("Mã giao dịch");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(input);

        new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("Chuyển khoản ngân hàng")
            .setMessage(message)
            .setView(layout)
            .setPositiveButton("Xác nhận", (d, w) -> {
                String transactionCode = input.getText().toString().trim();
                if (transactionCode.isEmpty()) {
                    cb.onFailure(PaymentResult.fail(getName(), "Chưa nhập mã giao dịch"));
                } else {
                    String transactionId = bankName + "_" + transactionCode;
                    cb.onSuccess(PaymentResult.ok(getName(), transactionId));
                }
            })
            .setNegativeButton("Hủy", (d, w) -> {
                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
            })
            .setCancelable(false)
            .show();
    }
}
