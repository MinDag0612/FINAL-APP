package com.FinalProject.feature_event_detail.model;

/**
 * Mô tả một hạng vé hiển thị trên màn hình chi tiết sự kiện.
 */
public class TicketTier {
    private final String label;
    private final long price;
    private final int total;
    private final int sold;

    public TicketTier(String label, long price, int total, int sold) {
        this.label = label;
        this.price = price;
        this.total = total;
        this.sold = sold;
    }

    public String getLabel() {
        return label;
    }

    public long getPrice() {
        return price;
    }

    public int getTotal() {
        return total;
    }

    public int getSold() {
        return sold;
    }

    public int getRemaining() {
        return Math.max(total - sold, 0);
    }
}
