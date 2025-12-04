package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model cho Promotion/Discount codes
 * Hỗ trợ early bird, member rewards, voucher codes
 */
public class Promotion {
    private String promotion_id;
    private String promotion_code;          // "EARLYBIRD2025", "MEMBER20"
    private String promotion_type;          // "percentage", "fixed_amount"
    private long discount_value;            // 20 (20% hoặc 20000 VNĐ)
    private String applicable_events;       // "all" hoặc event_id cụ thể
    private List<String> applicable_event_ids;  // Danh sách event IDs nếu không phải "all"
    private long valid_from;
    private long valid_until;
    private int usage_limit;                // Giới hạn số lần sử dụng
    private int usage_count;                // Số lần đã sử dụng
    private PromotionConditions conditions;
    private String description;
    private boolean is_active;

    public Promotion() {} // Firestore requires

    /**
     * Điều kiện áp dụng promotion
     */
    public static class PromotionConditions {
        private long min_purchase;          // Giá trị đơn hàng tối thiểu
        private String user_type;           // "all", "member", "new_user"
        private int min_tickets;            // Số vé tối thiểu
        private String payment_method;      // Phương thức thanh toán yêu cầu (optional)

        public PromotionConditions() {}

        public PromotionConditions(long min_purchase, String user_type, 
                                  int min_tickets, String payment_method) {
            this.min_purchase = min_purchase;
            this.user_type = user_type;
            this.min_tickets = min_tickets;
            this.payment_method = payment_method;
        }

        // Getters and Setters
        public long getMin_purchase() { return min_purchase; }
        public void setMin_purchase(long min_purchase) { this.min_purchase = min_purchase; }

        public String getUser_type() { return user_type; }
        public void setUser_type(String user_type) { this.user_type = user_type; }

        public int getMin_tickets() { return min_tickets; }
        public void setMin_tickets(int min_tickets) { this.min_tickets = min_tickets; }

        public String getPayment_method() { return payment_method; }
        public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("min_purchase", min_purchase);
            map.put("user_type", user_type);
            map.put("min_tickets", min_tickets);
            map.put("payment_method", payment_method);
            return map;
        }
    }

    // Getters and Setters
    public String getPromotion_id() { return promotion_id; }
    public void setPromotion_id(String promotion_id) { this.promotion_id = promotion_id; }

    public String getPromotion_code() { return promotion_code; }
    public void setPromotion_code(String promotion_code) { this.promotion_code = promotion_code; }

    public String getPromotion_type() { return promotion_type; }
    public void setPromotion_type(String promotion_type) { this.promotion_type = promotion_type; }

    public long getDiscount_value() { return discount_value; }
    public void setDiscount_value(long discount_value) { this.discount_value = discount_value; }

    public String getApplicable_events() { return applicable_events; }
    public void setApplicable_events(String applicable_events) { this.applicable_events = applicable_events; }

    public List<String> getApplicable_event_ids() { return applicable_event_ids; }
    public void setApplicable_event_ids(List<String> applicable_event_ids) { 
        this.applicable_event_ids = applicable_event_ids; 
    }

    public long getValid_from() { return valid_from; }
    public void setValid_from(long valid_from) { this.valid_from = valid_from; }

    public long getValid_until() { return valid_until; }
    public void setValid_until(long valid_until) { this.valid_until = valid_until; }

    public int getUsage_limit() { return usage_limit; }
    public void setUsage_limit(int usage_limit) { this.usage_limit = usage_limit; }

    public int getUsage_count() { return usage_count; }
    public void setUsage_count(int usage_count) { this.usage_count = usage_count; }

    public PromotionConditions getConditions() { return conditions; }
    public void setConditions(PromotionConditions conditions) { this.conditions = conditions; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean is_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("promotion_id", promotion_id);
        map.put("promotion_code", promotion_code);
        map.put("promotion_type", promotion_type);
        map.put("discount_value", discount_value);
        map.put("applicable_events", applicable_events);
        map.put("applicable_event_ids", applicable_event_ids);
        map.put("valid_from", valid_from);
        map.put("valid_until", valid_until);
        map.put("usage_limit", usage_limit);
        map.put("usage_count", usage_count);
        map.put("description", description);
        map.put("is_active", is_active);

        if (conditions != null) {
            map.put("conditions", conditions.toMap());
        }

        return map;
    }

    /**
     * Tính toán discount amount dựa trên order amount
     */
    public long calculateDiscount(long orderAmount) {
        if ("percentage".equals(promotion_type)) {
            return (orderAmount * discount_value) / 100;
        } else if ("fixed_amount".equals(promotion_type)) {
            return discount_value;
        }
        return 0;
    }

    /**
     * Kiểm tra promotion còn hạn không
     */
    public boolean isValid() {
        long now = System.currentTimeMillis();
        return is_active 
            && now >= valid_from 
            && now <= valid_until 
            && usage_count < usage_limit;
    }

    /**
     * Kiểm tra promotion có áp dụng cho event không
     */
    public boolean isApplicableToEvent(String eventId) {
        if ("all".equals(applicable_events)) {
            return true;
        }
        return applicable_event_ids != null && applicable_event_ids.contains(eventId);
    }
}
