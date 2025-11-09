package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Orders {
    private String user_id;
    private int total_price;
    private boolean payment_status;
    private List<TicketItem> tickets_list;
    private String qr_code;

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
                    .toList();
            map.put("tickets_list", ticketsMapList);
        }

        map.put("qr_code", qr_code);
        return map;
    }
}
