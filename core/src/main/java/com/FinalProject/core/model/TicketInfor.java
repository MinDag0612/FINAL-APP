package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.Map;

public class TicketInfor {
    private String tickets_class;     // ví dụ: "Premium", "VIP", "General"
    private int tickets_quantity;     // tổng số vé loại này
    private int tickets_sold;         // số vé đã bán
    private int tickets_price;        // giá 1 vé (VND)

    // ⚙️ Constructor mặc định (Firebase cần có)
    public TicketInfor() {}

    // ⚙️ Constructor đầy đủ
    public TicketInfor(String tickets_class, int tickets_quantity, int tickets_sold, int tickets_price) {
        this.tickets_class = tickets_class;
        this.tickets_quantity = tickets_quantity;
        this.tickets_sold = tickets_sold;
        this.tickets_price = tickets_price;
    }

    // 🧩 Getter & Setter
    public String getTickets_class() {
        return tickets_class;
    }

    public void setTickets_class(String tickets_class) {
        this.tickets_class = tickets_class;
    }

    public int getTickets_quantity() {
        return tickets_quantity;
    }

    public void setTickets_quantity(int tickets_quantity) {
        this.tickets_quantity = tickets_quantity;
    }

    public int getTickets_sold() {
        return tickets_sold;
    }

    public void setTickets_sold(int tickets_sold) {
        this.tickets_sold = tickets_sold;
    }

    public int getTickets_price() {
        return tickets_price;
    }

    public void setTickets_price(int tickets_price) {
        this.tickets_price = tickets_price;
    }

    // 🧭 Chuyển object thành Map để push lên Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tickets_class", tickets_class);
        map.put("tickets_quantity", tickets_quantity);
        map.put("tickets_sold", tickets_sold);
        map.put("tickets_price", tickets_price);
        return map;
    }
}
