package com.FinalProject.feature_booking.payment;

/**
 * Các phương thức thanh toán hỗ trợ
 */
public enum PaymentMethod {
    CARD,           // Thẻ tín dụng/ghi nợ
    WALLET,         // Ví điện tử (legacy - dùng chung MoMo + ZaloPay)
    QR,             // Quét mã QR
    MOMO,           // Ví MoMo
    ZALOPAY,        // Ví ZaloPay
    BANK_TRANSFER   // Chuyển khoản ngân hàng
}
