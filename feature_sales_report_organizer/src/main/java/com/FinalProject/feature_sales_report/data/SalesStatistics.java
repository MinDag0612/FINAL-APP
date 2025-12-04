package com.FinalProject.feature_sales_report.data;

/**
 * Model chứa thống kê doanh thu của sự kiện
 */
public class SalesStatistics {
    private long totalRevenue;          // Tổng doanh thu
    private int totalTicketsSold;       // Tổng số vé đã bán
    private int totalOrders;            // Tổng số đơn hàng
    private String mostPopularTicket;   // Loại vé bán chạy nhất

    public SalesStatistics() {
        this.totalRevenue = 0;
        this.totalTicketsSold = 0;
        this.totalOrders = 0;
        this.mostPopularTicket = "N/A";
    }

    public SalesStatistics(long totalRevenue, int totalTicketsSold, int totalOrders, String mostPopularTicket) {
        this.totalRevenue = totalRevenue;
        this.totalTicketsSold = totalTicketsSold;
        this.totalOrders = totalOrders;
        this.mostPopularTicket = mostPopularTicket;
    }

    public long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalTicketsSold() {
        return totalTicketsSold;
    }

    public void setTotalTicketsSold(int totalTicketsSold) {
        this.totalTicketsSold = totalTicketsSold;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public String getMostPopularTicket() {
        return mostPopularTicket;
    }

    public void setMostPopularTicket(String mostPopularTicket) {
        this.mostPopularTicket = mostPopularTicket;
    }
}
