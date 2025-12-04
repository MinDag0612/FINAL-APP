package com.FinalProject.feature_sales_report.data;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.model.TicketItem;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.core.util.TicketS_Infor_API;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository quản lý dữ liệu báo cáo bán vé
 */
public class SalesReportRepository {

    private static SalesReportRepository instance;

    private SalesReportRepository() {}

    public static synchronized SalesReportRepository getInstance() {
        if (instance == null) {
            instance = new SalesReportRepository();
        }
        return instance;
    }

    /**
     * Lấy tất cả đơn hàng của sự kiện
     */
    public Task<List<Orders>> getOrdersForEvent(String eventId) {
        return Order_API.getOrdersByEventId(eventId).continueWith(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                List<Orders> orders = new ArrayList<>();
                if (snapshot != null) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Orders order = doc.toObject(Orders.class);
                        if (order != null) {
                            orders.add(order);
                        }
                    }
                }
                return orders;
            }
            throw task.getException();
        });
    }

    /**
     * Lấy thông tin vé của sự kiện
     */
    public Task<List<TicketInfor>> getTicketsForEvent(String eventId) {
        return TicketS_Infor_API.getTicketInforByEventId(eventId).continueWith(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                List<TicketInfor> tickets = new ArrayList<>();
                if (snapshot != null) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        TicketInfor ticket = doc.toObject(TicketInfor.class);
                        if (ticket != null) {
                            tickets.add(ticket);
                        }
                    }
                }
                return tickets;
            }
            throw task.getException();
        });
    }

    /**
     * Tính toán thống kê doanh thu từ danh sách đơn hàng
     */
    public Task<SalesStatistics> calculateStatistics(String eventId) {
        Task<List<Orders>> ordersTask = getOrdersForEvent(eventId);
        Task<List<TicketInfor>> ticketsTask = getTicketsForEvent(eventId);

        return Tasks.whenAllSuccess(ordersTask, ticketsTask).continueWith(task -> {
            if (task.isSuccessful()) {
                List<Orders> orders = (List<Orders>) task.getResult().get(0);
                List<TicketInfor> tickets = (List<TicketInfor>) task.getResult().get(1);

                long totalRevenue = 0;
                int totalTicketsSold = 0;
                Map<String, Integer> ticketSales = new HashMap<>();

                // Tính từ orders
                for (Orders order : orders) {
                    totalRevenue += order.getTotal_price();
                    
                    // Count tickets from tickets_list
                    if (order.getTickets_list() != null) {
                        for (TicketItem item : order.getTickets_list()) {
                            totalTicketsSold += item.getQuantity();
                            String ticketId = item.getTickets_infor_id();
                            ticketSales.put(ticketId, 
                                ticketSales.getOrDefault(ticketId, 0) + item.getQuantity());
                        }
                    }
                }

                // Tìm loại vé bán chạy nhất từ tickets
                String mostPopular = "N/A";
                int maxSold = 0;
                for (TicketInfor ticket : tickets) {
                    if (ticket.getTickets_sold() > maxSold) {
                        maxSold = ticket.getTickets_sold();
                        mostPopular = ticket.getTickets_class();
                    }
                }

                return new SalesStatistics(
                    totalRevenue,
                    totalTicketsSold,
                    orders.size(),
                    mostPopular
                );
            }
            throw task.getException();
        });
    }

    /**
     * Lấy số lượng vé đã bán theo từng loại
     */
    public Task<Map<String, TicketSalesInfo>> getTicketSalesByClass(String eventId) {
        return getTicketsForEvent(eventId).continueWith(task -> {
            if (task.isSuccessful()) {
                List<TicketInfor> tickets = task.getResult();
                Map<String, TicketSalesInfo> salesMap = new HashMap<>();
                
                for (TicketInfor ticket : tickets) {
                    String ticketClass = ticket.getTickets_class();
                    int total = ticket.getTickets_quantity();
                    int sold = ticket.getTickets_sold();
                    int available = total - sold;
                    long price = ticket.getTickets_price();
                    
                    salesMap.put(ticketClass, new TicketSalesInfo(
                        ticketClass, total, sold, available, price
                    ));
                }
                
                return salesMap;
            }
            throw task.getException();
        });
    }

    /**
     * Inner class chứa thông tin bán vé theo loại
     */
    public static class TicketSalesInfo {
        public String ticketClass;
        public int totalQuantity;
        public int soldQuantity;
        public int availableQuantity;
        public long price;

        public TicketSalesInfo(String ticketClass, int totalQuantity, int soldQuantity, 
                               int availableQuantity, long price) {
            this.ticketClass = ticketClass;
            this.totalQuantity = totalQuantity;
            this.soldQuantity = soldQuantity;
            this.availableQuantity = availableQuantity;
            this.price = price;
        }
    }
}
