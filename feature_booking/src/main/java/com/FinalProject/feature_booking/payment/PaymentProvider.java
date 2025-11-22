package com.FinalProject.feature_booking.payment;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


public interface PaymentProvider {
    @NonNull String getName(); // "CARD" | "WALLET" | "QR"
    void start(@NonNull Fragment host, @NonNull PaymentRequest req, @NonNull PaymentCallback cb);
}