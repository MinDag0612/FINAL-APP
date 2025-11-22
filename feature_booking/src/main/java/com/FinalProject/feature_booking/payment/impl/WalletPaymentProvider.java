package com.FinalProject.feature_booking.payment.impl;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WalletPaymentProvider implements PaymentProvider {
    @NonNull @Override public String getName() { return "WALLET"; }

    @Override public void start(@NonNull Fragment host, @NonNull PaymentRequest req, @NonNull PaymentCallback cb) {
        final String[] wallets = {"MoMo", "ZaloPay"};
        final boolean[] done = { false };

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(host.requireContext())
                        .setTitle("Chọn ví điện tử")
                        .setItems(wallets, (d, which) -> {
                            if (!done[0]) {
                                done[0] = true;
                                String selected = wallets[which];
                                cb.onSuccess(PaymentResult.ok(getName(), selected + "-" + System.currentTimeMillis()));
                            }
                        })
                        .setNegativeButton("Hủy", (d, w) -> {
                            if (!done[0]) {
                                done[0] = true;
                                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
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
}
