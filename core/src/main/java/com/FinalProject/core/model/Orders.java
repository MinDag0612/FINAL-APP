package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Orders {
    private String user_id;
    private int total_price;
    private boolean payment_status;
    private List<TicketItem> tickets_list;
    private String qr_code;
    
    // Payment details
    private String payment_method;      // "CARD", "MOMO", "ZALOPAY", "BANK_TRANSFER"
    private String transaction_id;      // ID giao dịch từ payment provider
    private long payment_timestamp;     // Thời gian thanh toán
    
    // Group Booking integration
    private String group_booking_id;    // ID của group booking (nếu là group order)
    
    // Promotion integration
    private String promotion_id;        // ID của promotion được áp dụng
    private String promotion_code;      // Mã promotion (để hiển thị)
    private int discount_amount;        // Số tiền được giảm giá
    private int original_price;         // Giá gốc trước giảm giá

    public Orders() {} // default constructor

    public Orders(String user_id, int total_price, boolean payment_status, List<TicketItem> tickets_list, String qr_code) {
        this.user_id = user_id;
        this.total_price = total_price;
        this.payment_status = payment_status;
        this.tickets_list = tickets_list;
        this.qr_code = qr_code;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public int getTotal_price() {
        return total_price;
    }

    public void setTotal_price(int total_price) {
        this.total_price = total_price;
    }

    public boolean getPayment_status() {
        return payment_status;
    }

    public void setPayment_status(boolean payment_status) {
        this.payment_status = payment_status;
    }

    public List<TicketItem> getTickets_list() {
        return tickets_list;
    }

    public void setTickets_list(List<TicketItem> tickets_list) {
        this.tickets_list = tickets_list;
    }

    public String getQr_code() {
        return qr_code;
    }

    public void setQr_code(String qr_code) {
        this.qr_code = qr_code;
    }

    public String getPayment_method() {
        return payment_method;
    }

    public void setPayment_method(String payment_method) {
        this.payment_method = payment_method;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public long getPayment_timestamp() {
        return payment_timestamp;
    }

    public void setPayment_timestamp(long payment_timestamp) {
        this.payment_timestamp = payment_timestamp;
    }

    public String getGroup_booking_id() {
        return group_booking_id;
    }

    public void setGroup_booking_id(String group_booking_id) {
        this.group_booking_id = group_booking_id;
    }

    public String getPromotion_id() {
        return promotion_id;
    }

    public void setPromotion_id(String promotion_id) {
        this.promotion_id = promotion_id;
    }

    public String getPromotion_code() {
        return promotion_code;
    }

    public void setPromotion_code(String promotion_code) {
        this.promotion_code = promotion_code;
    }

    public int getDiscount_amount() {
        return discount_amount;
    }

    public void setDiscount_amount(int discount_amount) {
        this.discount_amount = discount_amount;
    }

    public int getOriginal_price() {
        return original_price;
    }

    public void setOriginal_price(int original_price) {
        this.original_price = original_price;
    }

    // Chuyển object thành Map để push lên Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", user_id);
        map.put("total_price", total_price);
        map.put("payment_status", payment_status);

        // Chuyển list TicketItem sang list Map
        if (tickets_list != null) {
            List<Map<String, Object>> ticketsMapList = tickets_list.stream()
                    .map(TicketItem::toMap)
                    .collect(Collectors.toList());
            map.put("tickets_list", ticketsMapList);
        }

        map.put("qr_code", qr_code);
        
        // Payment details
        map.put("payment_method", payment_method);
        map.put("transaction_id", transaction_id);
        map.put("payment_timestamp", payment_timestamp);
        
        // Group Booking integration
        map.put("group_booking_id", group_booking_id);
        
        // Promotion integration
        map.put("promotion_id", promotion_id);
        map.put("promotion_code", promotion_code);
        map.put("discount_amount", discount_amount);
        map.put("original_price", original_price);
        
        return map;
    }
}
