package com.FinalProject.core.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Promotion_API - Quản lý promotions và discount codes
 */
public class Promotion_API {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String PROMOTIONS_COLLECTION = "Promotions";
    private static final String PROMOTION_USAGE_COLLECTION = "PromotionUsage";

    /**
     * Lấy tất cả promotions đang active
     */
    public static Task<QuerySnapshot> getActivePromotions() {
        long now = System.currentTimeMillis();
        return db.collection(PROMOTIONS_COLLECTION)
                .whereEqualTo("is_active", true)
                .whereLessThanOrEqualTo("valid_from", now)
                .whereGreaterThanOrEqualTo("valid_until", now)
                .get();
    }

    /**
     * Lấy promotion theo code
     */
    public static Task<DocumentSnapshot> getPromotionByCode(@NonNull String promotionCode) {
        return db.collection(PROMOTIONS_COLLECTION)
                .whereEqualTo("promotion_code", promotionCode.toUpperCase())
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        return task.getResult().getDocuments().get(0);
                    }
                    throw new Exception("Mã khuyến mãi không tồn tại");
                });
    }

    /**
     * Lấy promotions áp dụng cho một event
     */
    public static Task<QuerySnapshot> getPromotionsForEvent(@NonNull String eventId) {
        long now = System.currentTimeMillis();
        return db.collection(PROMOTIONS_COLLECTION)
                .whereEqualTo("is_active", true)
                .whereLessThanOrEqualTo("valid_from", now)
                .whereGreaterThanOrEqualTo("valid_until", now)
                .get()
                .continueWith(task -> {
                    // Filter by applicable events in client side
                    // (Firestore doesn't support OR on different fields easily)
                    return task.getResult();
                });
    }

    /**
     * Validate promotion code
     * Kiểm tra xem code có hợp lệ và áp dụng được không
     */
    public static Task<Map<String, Object>> validatePromotion(
            @NonNull String promotionCode,
            @NonNull String eventId,
            @NonNull String userId,
            long orderAmount,
            int ticketCount) {

        return getPromotionByCode(promotionCode)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot promoDoc = task.getResult();
                    if (!promoDoc.exists()) {
                        throw new Exception("Mã khuyến mãi không tồn tại");
                    }

                    // Check active status
                    Boolean isActive = promoDoc.getBoolean("is_active");
                    if (isActive == null || !isActive) {
                        throw new Exception("Mã khuyến mãi đã hết hiệu lực");
                    }

                    // Check validity period
                    long now = System.currentTimeMillis();
                    Long validFrom = promoDoc.getLong("valid_from");
                    Long validUntil = promoDoc.getLong("valid_until");

                    if (validFrom == null || validUntil == null) {
                        throw new Exception("Mã khuyến mãi không hợp lệ");
                    }

                    if (now < validFrom) {
                        throw new Exception("Mã khuyến mãi chưa có hiệu lực");
                    }

                    if (now > validUntil) {
                        throw new Exception("Mã khuyến mãi đã hết hạn");
                    }

                    // Check usage limit
                    Long usageLimit = promoDoc.getLong("usage_limit");
                    Long usageCount = promoDoc.getLong("usage_count");

                    if (usageLimit != null && usageCount != null && usageCount >= usageLimit) {
                        throw new Exception("Mã khuyến mãi đã hết lượt sử dụng");
                    }

                    // Check if user already used this promotion
                    return checkUserUsage(promoDoc.getId(), userId)
                            .continueWith(usageTask -> {
                                if (usageTask.getResult()) {
                                    throw new Exception("Bạn đã sử dụng mã khuyến mãi này rồi");
                                }

                                // Check applicable events
                                String applicableEvents = promoDoc.getString("applicable_events");
                                if (!"all".equals(applicableEvents)) {
                                    // Check if eventId in applicable_event_ids array
                                    // (Simplified - should check the array)
                                }

                                // Check conditions
                                Map<String, Object> conditions = 
                                    (Map<String, Object>) promoDoc.get("conditions");

                                if (conditions != null) {
                                    Long minPurchase = (Long) conditions.get("min_purchase");
                                    if (minPurchase != null && orderAmount < minPurchase) {
                                        throw new Exception("Đơn hàng tối thiểu: " + 
                                            String.format("%,d VNĐ", minPurchase));
                                    }

                                    Long minTickets = (Long) conditions.get("min_tickets");
                                    if (minTickets != null && ticketCount < minTickets) {
                                        throw new Exception("Số vé tối thiểu: " + minTickets);
                                    }
                                }

                                // Calculate discount
                                String promotionType = promoDoc.getString("promotion_type");
                                Long discountValue = promoDoc.getLong("discount_value");

                                if (promotionType == null || discountValue == null) {
                                    throw new Exception("Mã khuyến mãi không hợp lệ");
                                }

                                long discountAmount;
                                if ("percentage".equals(promotionType)) {
                                    discountAmount = (orderAmount * discountValue) / 100;
                                } else {
                                    discountAmount = discountValue;
                                }

                                // Ensure discount doesn't exceed order amount
                                discountAmount = Math.min(discountAmount, orderAmount);

                                Map<String, Object> result = new HashMap<>();
                                result.put("promotion_id", promoDoc.getId());
                                result.put("promotion_code", promotionCode);
                                result.put("discount_amount", discountAmount);
                                result.put("final_amount", orderAmount - discountAmount);
                                result.put("description", promoDoc.getString("description"));

                                return result;
                            });
                });
    }

    /**
     * Áp dụng promotion - tăng usage count
     */
    public static Task<Void> applyPromotion(@NonNull String promotionId,
                                           @NonNull String userId,
                                           @NonNull String orderId) {
        // Increment usage count
        Task<Void> incrementTask = db.collection(PROMOTIONS_COLLECTION)
                .document(promotionId)
                .update("usage_count", FieldValue.increment(1));

        // Record user usage
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("promotion_id", promotionId);
        usageData.put("user_id", userId);
        usageData.put("order_id", orderId);
        usageData.put("used_at", System.currentTimeMillis());

        Task<Void> recordTask = db.collection(PROMOTION_USAGE_COLLECTION)
                .add(usageData)
                .continueWith(task -> null);

        return incrementTask.continueWithTask(task -> recordTask);
    }

    /**
     * Kiểm tra user đã sử dụng promotion chưa
     */
    private static Task<Boolean> checkUserUsage(@NonNull String promotionId,
                                               @NonNull String userId) {
        return db.collection(PROMOTION_USAGE_COLLECTION)
                .whereEqualTo("promotion_id", promotionId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return !task.getResult().isEmpty();
                    }
                    return false;
                });
    }

    /**
     * Tạo promotion mới (Admin only)
     */
    public static Task<String> createPromotion(@NonNull Map<String, Object> promotionData) {
        return db.collection(PROMOTIONS_COLLECTION)
                .add(promotionData)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().getId();
                    }
                    throw task.getException();
                });
    }

    /**
     * Deactivate promotion
     */
    public static Task<Void> deactivatePromotion(@NonNull String promotionId) {
        return db.collection(PROMOTIONS_COLLECTION)
                .document(promotionId)
                .update("is_active", false);
    }

    /**
     * Lấy promotions cho members (user type = "member")
     */
    public static Task<QuerySnapshot> getMemberPromotions() {
        long now = System.currentTimeMillis();
        return db.collection(PROMOTIONS_COLLECTION)
                .whereEqualTo("is_active", true)
                .whereLessThanOrEqualTo("valid_from", now)
                .whereGreaterThanOrEqualTo("valid_until", now)
                .whereEqualTo("conditions.user_type", "member")
                .get();
    }
}
