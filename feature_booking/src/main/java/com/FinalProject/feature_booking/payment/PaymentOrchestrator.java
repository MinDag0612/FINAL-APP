package com.FinalProject.feature_booking.payment;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.FinalProject.feature_booking.payment.impl.CardPaymentProvider;
import com.FinalProject.feature_booking.payment.impl.WalletPaymentProvider;
import com.FinalProject.feature_booking.payment.impl.QrPaymentProvider;


import java.util.EnumMap;
import java.util.Map;


public final class PaymentOrchestrator {


    private final Map<PaymentMethod, PaymentProvider> providers =
            new EnumMap<>(PaymentMethod.class);


    // Đăng ký provider tại đây
    public PaymentOrchestrator() {
        providers.put(PaymentMethod.CARD, new CardPaymentProvider());
        providers.put(PaymentMethod.WALLET, new WalletPaymentProvider());
        providers.put(PaymentMethod.QR, new QrPaymentProvider());
    }


    /** Cho phép tự chèn/ghi đè provider (DI/test). */
    public void register(@NonNull PaymentMethod method, @NonNull PaymentProvider provider) {
        providers.put(method, provider);
    }


    /** Gọi thanh toán theo method đã đăng ký. */
    public void pay(@NonNull Fragment host,
                    @NonNull PaymentMethod method,
                    @NonNull PaymentRequest req,
                    @NonNull PaymentCallback cb) {
        PaymentProvider p = providers.get(method);
        if (p == null) {
            cb.onFailure(PaymentResult.fail(method.name(), "Chưa có provider cho phương thức này"));
            return;
        }
        p.start(host, req, cb);
    }
}