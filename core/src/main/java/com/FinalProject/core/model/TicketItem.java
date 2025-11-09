package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.Map;

public class TicketItem {
    private String tickets_infor_id;
    private int quantity;

    public TicketItem() {} // default constructor cần cho Firestore

    public TicketItem(String tickets_infor_id, int quantity) {
        this.tickets_infor_id = tickets_infor_id;
        this.quantity = quantity;
    }

    public String getTickets_infor_id() {
        return tickets_infor_id;
    }

    public void setTickets_infor_id(String tickets_infor_id) {
        this.tickets_infor_id = tickets_infor_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    // Chuyển object thành Map để push lên Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tickets_infor_id", tickets_infor_id);
        map.put("quantity", quantity);
        return map;
    }
}
