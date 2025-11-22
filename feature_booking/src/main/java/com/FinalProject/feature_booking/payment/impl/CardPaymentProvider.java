package com.FinalProject.feature_booking.payment.impl;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.payment.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CardPaymentProvider implements PaymentProvider {

    @NonNull @Override public String getName() { return "CARD"; }

    @Override
    public void start(@NonNull Fragment host,
                      @NonNull PaymentRequest req,
                      @NonNull PaymentCallback cb) {
        if (!host.isAdded()) {
            cb.onFailure(PaymentResult.fail(getName(), "Màn hình không sẵn sàng"));
            return;
        }

        View view = LayoutInflater.from(host.requireContext())
                .inflate(R.layout.dialog_card_payment, null, false);

        TextInputLayout tilNumber = view.findViewById(R.id.til_card_number);
        TextInputLayout tilExp    = view.findViewById(R.id.til_card_exp);
        TextInputLayout tilCvc    = view.findViewById(R.id.til_card_cvc);
        TextInputEditText edtNumber = view.findViewById(R.id.edt_card_number);
        TextInputEditText edtExp    = view.findViewById(R.id.edt_card_exp);
        TextInputEditText edtCvc    = view.findViewById(R.id.edt_card_cvc);

        final boolean[] done = { false };

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(host.requireContext())
                        .setTitle("Thanh toán thẻ ngân hàng")
                        .setView(view)
                        .setPositiveButton("Thanh toán", null)
                        .setNegativeButton("Hủy", (d, w) -> {
                            if (!done[0]) {
                                done[0] = true;
                                cb.onFailure(PaymentResult.fail(getName(), "Người dùng hủy"));
                            }
                        })
                        .create();

        dialog.setCanceledOnTouchOutside(false);              // KHÔNG cho bấm ra ngoài
        dialog.setOnCancelListener(d -> {                     // Back/Cancel của hệ thống
            if (!done[0]) {
                done[0] = true;
                cb.onFailure(PaymentResult.fail(getName(), "Đã đóng hộp thoại"));
            }
        });
        dialog.setOnDismissListener(d -> {                    // Phòng trường hợp bị dismiss
            if (!done[0]) {
                done[0] = true;
                cb.onFailure(PaymentResult.fail(getName(), "Đã đóng hộp thoại"));
            }
        });

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                // clear errors
                if (tilNumber != null) tilNumber.setError(null);
                if (tilExp    != null) tilExp.setError(null);
                if (tilCvc    != null) tilCvc.setError(null);

                String num = edtNumber != null ? String.valueOf(edtNumber.getText()).trim() : "";
                String exp = edtExp    != null ? String.valueOf(edtExp.getText()).trim()    : "";
                String cvc = edtCvc    != null ? String.valueOf(edtCvc.getText()).trim()    : "";

                boolean ok = true;
                if (num.length() < 12) { if (tilNumber != null) tilNumber.setError("Số thẻ không hợp lệ"); ok = false; }
                if (!exp.matches("\\d{2}/\\d{2}")) { if (tilExp != null) tilExp.setError("MM/YY"); ok = false; }
                if (!cvc.matches("\\d{3}")) { if (tilCvc != null) tilCvc.setError("CVC 3 số"); ok = false; }

                if (!ok) return;

                if (!done[0]) {
                    done[0] = true;
                    dialog.dismiss();
                    cb.onSuccess(PaymentResult.ok(getName(), "CARD-" + System.currentTimeMillis()));
                }
            });
        });

        dialog.show();
    }
}
