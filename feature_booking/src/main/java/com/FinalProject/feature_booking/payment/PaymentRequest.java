// PaymentRequest.java
package com.FinalProject.feature_booking.payment;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;


public final class PaymentRequest {
    public final String eventId;
    public final String showId;
    private final String currency;
    private final String promo;
    private final ArrayList<String> seats;
    private final long amount; // VND
    @Nullable private final Bundle meta;


    public PaymentRequest(String eventId, String showId,
                          ArrayList<String> seats, long amount,
                          String currency, String promo,
                          @Nullable Bundle meta) {
        this.eventId = eventId;
        this.showId = showId;
        this.seats = seats != null ? new ArrayList<>(seats) : new ArrayList<>();
        this.amount = amount;
        this.currency = (currency == null || currency.isEmpty()) ? "VND" : currency;
        this.promo = promo;
        this.meta = meta;
    }


    public long getAmount() { return amount; }
    @NonNull public String getCurrency() { return currency; }
    @NonNull public ArrayList<String> getSeats() { return new ArrayList<>(seats); }
    @Nullable public String getPromo() { return promo; }
    @Nullable public Bundle getMeta() { return meta; }
}