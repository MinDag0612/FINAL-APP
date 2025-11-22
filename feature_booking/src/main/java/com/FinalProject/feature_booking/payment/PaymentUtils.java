package com.FinalProject.feature_booking.payment;


import java.util.Calendar;


public final class PaymentUtils {
    private PaymentUtils() {}


    // Luhn check
    public static boolean isLuhnValid(String number) {
        int sum = 0; boolean alt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char c = number.charAt(i);
            if (c < '0' || c > '9') return false;
            int n = c - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }


    // MM/YY và phải >= tháng hiện tại
    public static boolean isValidExpiry(String mmYY) {
        if (mmYY == null || mmYY.length() != 5 || mmYY.charAt(2) != '/') return false;
        try {
            int mm = Integer.parseInt(mmYY.substring(0, 2));
            int yy = Integer.parseInt(mmYY.substring(3, 5)); // 00..99
            if (mm < 1 || mm > 12) return false;


            Calendar now = Calendar.getInstance();
            int curYY = now.get(Calendar.YEAR) % 100;
            int curMM = now.get(Calendar.MONTH) + 1;


            if (yy < curYY) return false;
            return !(yy == curYY && mm < curMM);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}