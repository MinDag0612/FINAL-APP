package com.FinalProject.feature_booking.payment;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public final class PaymentResult {


    public enum Status { SUCCESS, FAILURE }


    @NonNull private final Status status;
    @NonNull private final String provider; // "CARD" | "WALLET" | "QR" | cổng khác
    @Nullable private final String transactionId;
    @Nullable private final String message; // mô tả lỗi nếu FAILURE
    @Nullable private final Bundle extras;


    private PaymentResult(@NonNull Status status,
                          @NonNull String provider,
                          @Nullable String transactionId,
                          @Nullable String message,
                          @Nullable Bundle extras) {
        this.status = status;
        this.provider = provider;
        this.transactionId = transactionId;
        this.message = message;
        this.extras = extras;
    }


    // Factories
    @NonNull public static PaymentResult ok(@NonNull String provider, @NonNull String txId) {
        return new PaymentResult(Status.SUCCESS, provider, txId, null, null);
    }
    @NonNull public static PaymentResult ok(@NonNull String provider, @NonNull String txId, @Nullable Bundle extras) {
        return new PaymentResult(Status.SUCCESS, provider, txId, null, extras);
    }
    @NonNull public static PaymentResult fail(@NonNull String provider, @NonNull String message) {
        return new PaymentResult(Status.FAILURE, provider, null, message, null);
    }
    @NonNull public static PaymentResult fail(@NonNull String provider, @NonNull String message, @Nullable Bundle extras) {
        return new PaymentResult(Status.FAILURE, provider, null, message, extras);
    }


    // Getters
    @NonNull public Status getStatus() { return status; }
    @NonNull public String getProvider() { return provider; }
    @Nullable public String getTransactionId() { return transactionId; }
    @Nullable public String getMessage() { return message; }
    @Nullable public Bundle getExtras() { return extras; }
}