package com.FinalProject.feature_home.model;

public class RecentTicketInfo {
    private final String title;
    private final String subtitle;
    private final boolean hasTicket;

    public RecentTicketInfo(String title, String subtitle, boolean hasTicket) {
        this.title = title;
        this.subtitle = subtitle;
        this.hasTicket = hasTicket;
    }

    public static RecentTicketInfo empty() {
        return new RecentTicketInfo(
                "Bạn chưa có vé nào",
                "Khi đặt vé, chúng tôi sẽ hiển thị ở đây.",
                false
        );
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean hasTicket() {
        return hasTicket;
    }
}
