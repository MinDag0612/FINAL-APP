package com.FinalProject.feature_sales_report.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.model.Orders;
import com.FinalProject.feature_sales_report.R;
import com.FinalProject.feature_sales_report.data.SalesReportRepository;
import com.FinalProject.feature_sales_report.data.SalesStatistics;
import com.FinalProject.feature_sales_report.domain.GetOrdersUseCase;
import com.FinalProject.feature_sales_report.domain.GetStatisticsUseCase;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity hiển thị báo cáo bán vé cho Organizer
 */
public class SalesReportActivity extends AppCompatActivity {

    private static final String TAG = "SalesReportActivity";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_NAME = "event_name";

    private TextView tvEventName;
    private TextView tvTotalRevenue, tvTotalTickets, tvTotalOrders, tvMostPopular;
    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private TextView tvEmptyOrders;

    private OrderAdapter adapter;
    private SalesReportRepository repository;
    private GetStatisticsUseCase getStatisticsUseCase;
    private GetOrdersUseCase getOrdersUseCase;

    private String eventId;
    private String eventName;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_report);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);

        if (TextUtils.isEmpty(eventId)) {
            // Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initRepository();
        loadData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvEventName = findViewById(R.id.tv_event_name);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvTotalTickets = findViewById(R.id.tv_total_tickets);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvMostPopular = findViewById(R.id.tv_most_popular);
        rvOrders = findViewById(R.id.rv_orders);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyOrders = findViewById(R.id.tv_empty_orders);

        btnBack.setOnClickListener(v -> finish());
        tvEventName.setText(eventName != null ? eventName : "Báo cáo bán vé");

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        rvOrders.setAdapter(adapter);
    }

    private void initRepository() {
        repository = SalesReportRepository.getInstance();
        getStatisticsUseCase = new GetStatisticsUseCase(repository);
        getOrdersUseCase = new GetOrdersUseCase(repository);
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        // Load statistics
        getStatisticsUseCase.execute(eventId)
                .addOnSuccessListener(stats -> {
                    displayStatistics(stats);
                    loadOrders();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading statistics", e);
                    // Toast.makeText(this, "Lỗi tải thống kê", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayStatistics(SalesStatistics stats) {
        tvTotalRevenue.setText(currencyFormat.format(stats.getTotalRevenue()));
        tvTotalTickets.setText(String.valueOf(stats.getTotalTicketsSold()));
        tvTotalOrders.setText(String.valueOf(stats.getTotalOrders()));
        tvMostPopular.setText(stats.getMostPopularTicket());
    }

    private void loadOrders() {
        getOrdersUseCase.execute(eventId)
                .addOnSuccessListener(orders -> {
                    progressBar.setVisibility(View.GONE);
                    if (orders == null || orders.isEmpty()) {
                        tvEmptyOrders.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setOrders(orders);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyOrders.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading orders", e);
                    // Toast.makeText(this, "Lỗi tải danh sách đơn hàng", Toast.LENGTH_SHORT).show();
                });
    }

    // RecyclerView Adapter for Orders
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

        private List<Orders> orders = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        void setOrders(List<Orders> orders) {
            this.orders = orders;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            holder.bind(orders.get(position));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvTicketClass, tvQuantity, tvTotalPrice, tvDate, tvStatus;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tv_order_id);
                tvTicketClass = itemView.findViewById(R.id.tv_ticket_class);
                tvQuantity = itemView.findViewById(R.id.tv_quantity);
                tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }

            void bind(Orders order) {
                tvOrderId.setText("Đơn #" + order.getUser_id().substring(0, Math.min(8, order.getUser_id().length())));
                
                // Get first ticket class from tickets_list
                String ticketClass = "N/A";
                int quantity = 0;
                if (order.getTickets_list() != null && !order.getTickets_list().isEmpty()) {
                    quantity = order.getTickets_list().size();
                    ticketClass = "Vé";
                }
                
                tvTicketClass.setText(ticketClass);
                tvQuantity.setText("x" + quantity);
                tvTotalPrice.setText(currencyFormat.format(order.getTotal_price()));
                
                tvDate.setText("N/A");
                
                String status = order.getPayment_status() ? "Đã thanh toán" : "Chưa thanh toán";
                tvStatus.setText(status);
                
                // Color code status
                if (order.getPayment_status()) {
                    tvStatus.setTextColor(0xFF4CAF50);
                } else {
                    tvStatus.setTextColor(0xFFFF9800);
                }
            }
        }
    }
}
