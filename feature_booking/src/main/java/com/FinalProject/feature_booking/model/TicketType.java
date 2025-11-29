package com.FinalProject.feature_booking.model;

import java.io.Serializable;

public class TicketType implements Serializable {
    private String typeId;
    private String name;   // STD / VIP / VVIP
    private Long price;    // Long (đồng)
    private Long quota;
    private Long sold;

    public TicketType() {}

    public TicketType(String typeId, String name, Long price, Long quota, Long sold) {
        this.typeId = typeId; this.name = name; this.price = price; this.quota = quota; this.sold = sold;
    }

    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public Long getQuota() { return quota; }
    public void setQuota(Long quota) { this.quota = quota; }

    public Long getSold() { return sold; }
    public void setSold(Long sold) { this.sold = sold; }

    public long getPriceSafe()  { return price == null ? 0L : price; }
    public long getQuotaSafe()  { return quota == null ? 0L : quota; }
    public long getSoldSafe()   { return sold  == null ? 0L : sold;  }
    public long getRemaining()  { return Math.max(0L, getQuotaSafe() - getSoldSafe()); }
    public boolean isSoldOut()  { return getRemaining() <= 0L; }

    @Override public String toString() {
        return "TicketType{" +
                "typeId='" + typeId + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quota=" + quota +
                ", sold=" + sold +
                '}';
    }
}
