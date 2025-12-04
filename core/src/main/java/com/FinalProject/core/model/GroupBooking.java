package com.FinalProject.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model cho Group Booking - Đặt vé nhóm
 * Cho phép nhiều người cùng đặt vé và chia sẻ thanh toán
 */
public class GroupBooking {
    private String group_booking_id;
    private String event_id;
    private String creator_id;          // User tạo group booking
    private String event_name;
    private long total_amount;
    private String status;              // "pending", "confirmed", "cancelled"
    private long created_at;
    private long expires_at;            // Thời hạn thanh toán
    private List<Participant> participants;
    private int max_participants;

    public GroupBooking() {} // Firestore requires

    public GroupBooking(String group_booking_id, String event_id, String creator_id,
                        String event_name, long total_amount, String status,
                        long created_at, long expires_at, List<Participant> participants,
                        int max_participants) {
        this.group_booking_id = group_booking_id;
        this.event_id = event_id;
        this.creator_id = creator_id;
        this.event_name = event_name;
        this.total_amount = total_amount;
        this.status = status;
        this.created_at = created_at;
        this.expires_at = expires_at;
        this.participants = participants;
        this.max_participants = max_participants;
    }

    /**
     * Participant - Người tham gia group booking
     */
    public static class Participant {
        private String user_id;
        private String user_name;
        private long contribution;      // Số tiền đóng góp
        private boolean payment_status; // Đã thanh toán chưa
        private long paid_at;
        private String payment_method;

        public Participant() {}

        public Participant(String user_id, String user_name, long contribution,
                          boolean payment_status, long paid_at, String payment_method) {
            this.user_id = user_id;
            this.user_name = user_name;
            this.contribution = contribution;
            this.payment_status = payment_status;
            this.paid_at = paid_at;
            this.payment_method = payment_method;
        }

        // Getters and Setters
        public String getUser_id() { return user_id; }
        public void setUser_id(String user_id) { this.user_id = user_id; }

        public String getUser_name() { return user_name; }
        public void setUser_name(String user_name) { this.user_name = user_name; }

        public long getContribution() { return contribution; }
        public void setContribution(long contribution) { this.contribution = contribution; }

        public boolean isPayment_status() { return payment_status; }
        public void setPayment_status(boolean payment_status) { this.payment_status = payment_status; }

        public long getPaid_at() { return paid_at; }
        public void setPaid_at(long paid_at) { this.paid_at = paid_at; }

        public String getPayment_method() { return payment_method; }
        public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("user_id", user_id);
            map.put("user_name", user_name);
            map.put("contribution", contribution);
            map.put("payment_status", payment_status);
            map.put("paid_at", paid_at);
            map.put("payment_method", payment_method);
            return map;
        }
    }

    // Getters and Setters
    public String getGroup_booking_id() { return group_booking_id; }
    public void setGroup_booking_id(String group_booking_id) { this.group_booking_id = group_booking_id; }

    public String getEvent_id() { return event_id; }
    public void setEvent_id(String event_id) { this.event_id = event_id; }

    public String getCreator_id() { return creator_id; }
    public void setCreator_id(String creator_id) { this.creator_id = creator_id; }

    public String getEvent_name() { return event_name; }
    public void setEvent_name(String event_name) { this.event_name = event_name; }

    public long getTotal_amount() { return total_amount; }
    public void setTotal_amount(long total_amount) { this.total_amount = total_amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) { this.created_at = created_at; }

    public long getExpires_at() { return expires_at; }
    public void setExpires_at(long expires_at) { this.expires_at = expires_at; }

    public List<Participant> getParticipants() { return participants; }
    public void setParticipants(List<Participant> participants) { this.participants = participants; }

    public int getMax_participants() { return max_participants; }
    public void setMax_participants(int max_participants) { this.max_participants = max_participants; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("group_booking_id", group_booking_id);
        map.put("event_id", event_id);
        map.put("creator_id", creator_id);
        map.put("event_name", event_name);
        map.put("total_amount", total_amount);
        map.put("status", status);
        map.put("created_at", created_at);
        map.put("expires_at", expires_at);
        map.put("max_participants", max_participants);

        if (participants != null) {
            List<Map<String, Object>> participantMaps = new ArrayList<>();
            for (Participant p : participants) {
                participantMaps.add(p.toMap());
            }
            map.put("participants", participantMaps);
        }

        return map;
    }

    /**
     * Helper methods
     */
    public long getTotalPaid() {
        if (participants == null) return 0;
        long total = 0;
        for (Participant p : participants) {
            if (p.isPayment_status()) {
                total += p.getContribution();
            }
        }
        return total;
    }

    public long getRemainingAmount() {
        return total_amount - getTotalPaid();
    }

    public boolean isFullyPaid() {
        return getTotalPaid() >= total_amount;
    }

    public int getPaidParticipantsCount() {
        if (participants == null) return 0;
        int count = 0;
        for (Participant p : participants) {
            if (p.isPayment_status()) count++;
        }
        return count;
    }
}
