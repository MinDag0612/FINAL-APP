package com.FinalProject.core.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GroupBooking_API - Quản lý đặt vé nhóm
 * 
 * Collections:
 * - GroupBookings: Danh sách các group booking
 * - Orders: Thêm field group_booking_id để liên kết
 */
public class GroupBooking_API {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String GROUP_BOOKINGS_COLLECTION = "GroupBookings";

    /**
     * Tạo group booking mới
     */
    public static Task<String> createGroupBooking(@NonNull Map<String, Object> groupBookingData) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .add(groupBookingData)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().getId();
                    }
                    throw task.getException();
                });
    }

    /**
     * Lấy group booking theo ID
     */
    public static Task<DocumentSnapshot> getGroupBookingById(@NonNull String groupBookingId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .get();
    }

    /**
     * Lấy danh sách group bookings của user (làm creator)
     */
    public static Task<QuerySnapshot> getGroupBookingsByCreator(@NonNull String userId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .whereEqualTo("creator_id", userId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy danh sách group bookings mà user tham gia
     */
    public static Task<QuerySnapshot> getGroupBookingsByParticipant(@NonNull String userId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .whereArrayContains("participants", userId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy group bookings cho một event cụ thể
     */
    public static Task<QuerySnapshot> getGroupBookingsByEvent(@NonNull String eventId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "pending")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get();
    }

    /**
     * Thêm participant vào group booking
     */
    public static Task<Void> addParticipant(@NonNull String groupBookingId,
                                            @NonNull Map<String, Object> participantData) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .update("participants", FieldValue.arrayUnion(participantData));
    }

    /**
     * Xóa participant khỏi group booking
     */
    public static Task<Void> removeParticipant(@NonNull String groupBookingId,
                                               @NonNull String userId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    List<Map<String, Object>> participants = 
                        (List<Map<String, Object>>) doc.get("participants");

                    if (participants != null) {
                        participants.removeIf(p -> userId.equals(p.get("user_id")));
                        return db.collection(GROUP_BOOKINGS_COLLECTION)
                                .document(groupBookingId)
                                .update("participants", participants);
                    }
                    return Tasks.forResult(null);
                });
    }

    /**
     * Cập nhật payment status của participant
     */
    public static Task<Void> updateParticipantPayment(@NonNull String groupBookingId,
                                                      @NonNull String userId,
                                                      @NonNull String paymentMethod) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    List<Map<String, Object>> participants = 
                        (List<Map<String, Object>>) doc.get("participants");

                    if (participants != null) {
                        for (Map<String, Object> p : participants) {
                            if (userId.equals(p.get("user_id"))) {
                                p.put("payment_status", true);
                                p.put("paid_at", System.currentTimeMillis());
                                p.put("payment_method", paymentMethod);
                                break;
                            }
                        }
                        return db.collection(GROUP_BOOKINGS_COLLECTION)
                                .document(groupBookingId)
                                .update("participants", participants);
                    }
                    return Tasks.forResult(null);
                });
    }

    /**
     * Cập nhật status của group booking
     */
    public static Task<Void> updateStatus(@NonNull String groupBookingId,
                                         @NonNull String status) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .update("status", status);
    }

    /**
     * Xác nhận group booking khi đủ người thanh toán
     * Tạo orders cho tất cả participants
     */
    public static Task<Void> confirmGroupBooking(@NonNull String groupBookingId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    List<Map<String, Object>> participants = 
                        (List<Map<String, Object>>) doc.get("participants");
                    String eventId = doc.getString("event_id");

                    WriteBatch batch = db.batch();

                    // Update group booking status
                    DocumentReference groupBookingRef = db.collection(GROUP_BOOKINGS_COLLECTION)
                            .document(groupBookingId);
                    batch.update(groupBookingRef, "status", "confirmed");

                    // Create orders for each paid participant
                    if (participants != null) {
                        for (Map<String, Object> p : participants) {
                            Boolean paymentStatus = (Boolean) p.get("payment_status");
                            if (paymentStatus != null && paymentStatus) {
                                Map<String, Object> orderData = new HashMap<>();
                                orderData.put("user_id", p.get("user_id"));
                                orderData.put("event_id", eventId);
                                orderData.put("total_price", p.get("contribution"));
                                orderData.put("payment_status", true);
                                orderData.put("payment_method", p.get("payment_method"));
                                orderData.put("group_booking_id", groupBookingId);
                                orderData.put("created_at", System.currentTimeMillis());

                                DocumentReference orderRef = db.collection("Orders").document();
                                batch.set(orderRef, orderData);
                            }
                        }
                    }

                    return batch.commit();
                });
    }

    /**
     * Hủy group booking
     */
    public static Task<Void> cancelGroupBooking(@NonNull String groupBookingId) {
        return updateStatus(groupBookingId, "cancelled");
    }

    /**
     * Kiểm tra xem group booking đã đủ người thanh toán chưa
     */
    public static Task<Boolean> isFullyPaid(@NonNull String groupBookingId) {
        return db.collection(GROUP_BOOKINGS_COLLECTION)
                .document(groupBookingId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        return false;
                    }

                    DocumentSnapshot doc = task.getResult();
                    Long totalAmount = doc.getLong("total_amount");
                    List<Map<String, Object>> participants = 
                        (List<Map<String, Object>>) doc.get("participants");

                    if (totalAmount == null || participants == null) {
                        return false;
                    }

                    long paidAmount = 0;
                    for (Map<String, Object> p : participants) {
                        Boolean paymentStatus = (Boolean) p.get("payment_status");
                        if (paymentStatus != null && paymentStatus) {
                            Long contribution = (Long) p.get("contribution");
                            if (contribution != null) {
                                paidAmount += contribution;
                            }
                        }
                    }

                    return paidAmount >= totalAmount;
                });
    }

    /**
     * Gửi lời mời tham gia group booking
     * (Có thể tích hợp với Firebase Cloud Messaging sau)
     */
    public static Task<Void> sendInvitation(@NonNull String groupBookingId,
                                           @NonNull String inviteeUserId) {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("group_booking_id", groupBookingId);
        invitation.put("invitee_user_id", inviteeUserId);
        invitation.put("sent_at", System.currentTimeMillis());
        invitation.put("status", "pending");

        return db.collection("GroupBookingInvitations")
                .add(invitation)
                .continueWith(task -> null);
    }
}
