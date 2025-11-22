package com.FinalProject.feature_booking.payment;


    public interface PaymentCallback {
    void onSuccess(PaymentResult result);
    void onFailure(PaymentResult result);
}