package com.FinalProject.feature_ticket_manager.presentation;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_ticket_manager.R;
import com.FinalProject.feature_ticket_manager.data.TicketManagerRepository;
import com.FinalProject.feature_ticket_manager.domain.GetTicketsUseCase;
import com.FinalProject.feature_ticket_manager.domain.UpdateTicketUseCase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity quản lý vé cho Organizer
 * - Xem danh sách loại vé
 * - Thêm/sửa/xóa loại vé
 * - Cập nhật giá, số lượng
 */
public class TicketManagerActivity extends AppCompatActivity {

    private static final String TAG = "TicketManagerActivity";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_NAME = "event_name";

    private RecyclerView rvTickets;
    private ProgressBar progressBar;
    private TextView tvEventName;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private TicketAdapter adapter;

    private TicketManagerRepository repository;
    private GetTicketsUseCase getTicketsUseCase;
    private UpdateTicketUseCase updateTicketUseCase;

    private String eventId;
    private String eventName;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_manager);

        // Get data from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);

        if (TextUtils.isEmpty(eventId)) {
            // Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initRepository();
        loadTickets();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvEventName = findViewById(R.id.tv_event_name);
        rvTickets = findViewById(R.id.rv_tickets);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        fabAdd = findViewById(R.id.fab_add_ticket);

        btnBack.setOnClickListener(v -> finish());
        tvEventName.setText(eventName != null ? eventName : "Quản lý vé");

        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketAdapter();
        rvTickets.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddTicketDialog());
    }

    private void initRepository() {
        repository = TicketManagerRepository.getInstance();
        getTicketsUseCase = new GetTicketsUseCase(repository);
        updateTicketUseCase = new UpdateTicketUseCase(repository);
    }

    private void loadTickets() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        getTicketsUseCase.execute(eventId)
                .addOnSuccessListener(tickets -> {
                    progressBar.setVisibility(View.GONE);
                    if (tickets == null || tickets.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setTickets(tickets);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading tickets", e);
                    // Toast.makeText(this, "Lỗi tải danh sách vé", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddTicketDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_ticket, null);
        EditText etClass = dialogView.findViewById(R.id.et_ticket_class);
        EditText etQuantity = dialogView.findViewById(R.id.et_quantity);
        EditText etPrice = dialogView.findViewById(R.id.et_price);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm loại vé mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String ticketClass = etClass.getText().toString().trim();
                    String quantityStr = etQuantity.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();

                    if (TextUtils.isEmpty(ticketClass) || TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
                        // Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(quantityStr);
                    int price = Integer.parseInt(priceStr);

                    TicketInfor newTicket = new TicketInfor(ticketClass, quantity, 0, price);
                    
                    repository.addTicket(eventId, newTicket)
                            .addOnSuccessListener(aVoid -> {
                                // Toast.makeText(this, "Thêm vé thành công", Toast.LENGTH_SHORT).show();
                                loadTickets();
                            })
                            .addOnFailureListener(e -> {
                                // Toast.makeText(this, "Lỗi thêm vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditTicketDialog(TicketInfor ticket, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_ticket, null);
        EditText etClass = dialogView.findViewById(R.id.et_ticket_class);
        EditText etQuantity = dialogView.findViewById(R.id.et_quantity);
        EditText etPrice = dialogView.findViewById(R.id.et_price);

        etClass.setText(ticket.getTickets_class());
        etQuantity.setText(String.valueOf(ticket.getTickets_quantity()));
        etPrice.setText(String.valueOf(ticket.getTickets_price()));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Chỉnh sửa vé")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String ticketClass = etClass.getText().toString().trim();
                    String quantityStr = etQuantity.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();

                    if (TextUtils.isEmpty(ticketClass) || TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
                        // Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(quantityStr);
                    int price = Integer.parseInt(priceStr);

                    ticket.setTickets_class(ticketClass);
                    ticket.setTickets_quantity(quantity);
                    ticket.setTickets_price(price);

                    // Note: We need ticket document ID to update
                    // In a real implementation, you'd store the document ID with the ticket
                    // Toast.makeText(this, "Chức năng chỉnh sửa đang được phát triển", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // RecyclerView Adapter
    private class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

        private List<TicketInfor> tickets = new ArrayList<>();

        void setTickets(List<TicketInfor> tickets) {
            this.tickets = tickets;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ticket, parent, false);
            return new TicketViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
            holder.bind(tickets.get(position), position);
        }

        @Override
        public int getItemCount() {
            return tickets.size();
        }

        class TicketViewHolder extends RecyclerView.ViewHolder {
            TextView tvClass, tvQuantity, tvPrice, tvSold, tvAvailable;
            Button btnEdit;

            TicketViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClass = itemView.findViewById(R.id.tv_ticket_class);
                tvQuantity = itemView.findViewById(R.id.tv_quantity);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvSold = itemView.findViewById(R.id.tv_sold);
                tvAvailable = itemView.findViewById(R.id.tv_available);
                btnEdit = itemView.findViewById(R.id.btn_edit);
            }

            void bind(TicketInfor ticket, int position) {
                tvClass.setText(ticket.getTickets_class());
                tvQuantity.setText("Tổng: " + ticket.getTickets_quantity());
                tvPrice.setText(currencyFormat.format(ticket.getTickets_price()));
                tvSold.setText("Đã bán: " + ticket.getTickets_sold());
                
                int available = ticket.getTickets_quantity() - ticket.getTickets_sold();
                tvAvailable.setText("Còn lại: " + available);

                btnEdit.setOnClickListener(v -> showEditTicketDialog(ticket, position));
            }
        }
    }
}
