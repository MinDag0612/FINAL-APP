package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.feature_booking.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity để tạo Group Booking - Đặt vé nhóm
 * User nhập email bạn bè + số vé cho mỗi người
 */
public class GroupBookingActivity extends AppCompatActivity {

    // Simple Invitee model
    public static class Invitee {
        String email;
        int ticketCount;
        String status;

        public Invitee(String email, int ticketCount, String status) {
            this.email = email;
            this.ticketCount = ticketCount;
            this.status = status;
        }
    }

    private String eventId;
    private TextInputEditText etEmail;
    private TextInputEditText etTickets;
    private Button btnAddInvitee;
    private RecyclerView rvInvitees;
    private TextView tvTotalTickets;
    private Button btnCreateGroupBooking;

    private InviteeAdapter adapter;
    private List<Invitee> invitees = new ArrayList<>();
    private int totalTickets = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_booking);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            // Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_group_booking);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đặt vé nhóm");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etEmail = findViewById(R.id.et_invitee_email);
        etTickets = findViewById(R.id.et_invitee_tickets);
        btnAddInvitee = findViewById(R.id.btn_add_invitee);
        rvInvitees = findViewById(R.id.rv_invitees);
        tvTotalTickets = findViewById(R.id.tv_total_tickets);
        btnCreateGroupBooking = findViewById(R.id.btn_create_group_booking);
    }

    private void setupRecyclerView() {
        adapter = new InviteeAdapter(invitees, this::removeInvitee);
        rvInvitees.setLayoutManager(new LinearLayoutManager(this));
        rvInvitees.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddInvitee.setOnClickListener(v -> addInvitee());
        btnCreateGroupBooking.setOnClickListener(v -> createGroupBooking());
    }

    private void addInvitee() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String ticketsStr = etTickets.getText() != null ? etTickets.getText().toString().trim() : "";

        if (email.isEmpty()) {
            // Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            // Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        int tickets;
        try {
            tickets = Integer.parseInt(ticketsStr);
            if (tickets <= 0) {
                // Toast.makeText(this, "Số vé phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            // Toast.makeText(this, "Số vé không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra email trùng
        for (Invitee inv : invitees) {
            if (inv.email.equalsIgnoreCase(email)) {
                // Toast.makeText(this, "Email đã được thêm", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Invitee invitee = new Invitee(email, tickets, "PENDING");
        invitees.add(invitee);
        totalTickets += tickets;

        adapter.notifyItemInserted(invitees.size() - 1);
        updateTotalTickets();

        // Clear inputs
        etEmail.setText("");
        etTickets.setText("");
    }

    private void removeInvitee(int position) {
        if (position >= 0 && position < invitees.size()) {
            Invitee removed = invitees.remove(position);
            totalTickets -= removed.ticketCount;
            adapter.notifyItemRemoved(position);
            updateTotalTickets();
        }
    }

    private void updateTotalTickets() {
        tvTotalTickets.setText("Tổng số vé: " + totalTickets);
        btnCreateGroupBooking.setEnabled(totalTickets > 0);
    }

    private void createGroupBooking() {
        if (invitees.isEmpty()) {
            // Toast.makeText(this, "Vui lòng thêm ít nhất 1 người", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuthHelper.getCurrentUserUid();
        if (userId == null || userId.isEmpty()) {
            // Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateGroupBooking.setEnabled(false);
        btnCreateGroupBooking.setText("Đang tạo...");

        // Tạo data để lưu vào Firestore
        Map<String, Object> groupBookingData = new HashMap<>();
        groupBookingData.put("event_id", eventId);
        groupBookingData.put("creator_id", userId);
        groupBookingData.put("total_tickets", totalTickets);
        groupBookingData.put("created_at", System.currentTimeMillis());
        groupBookingData.put("status", "PENDING");
        
        List<Map<String, Object>> inviteesList = new ArrayList<>();
        for (Invitee inv : invitees) {
            Map<String, Object> invData = new HashMap<>();
            invData.put("email", inv.email);
            invData.put("ticket_count", inv.ticketCount);
            invData.put("status", inv.status);
            inviteesList.add(invData);
        }
        groupBookingData.put("invitees", inviteesList);

        FirebaseFirestore.getInstance().collection("GroupBookings")
            .add(groupBookingData)
            .addOnSuccessListener(docRef -> {
                // Toast.makeText(GroupBookingActivity.this, "Tạo nhóm thành công! ID: " + docRef.getId(), Toast.LENGTH_LONG).show();
                finish();
            })
            .addOnFailureListener(e -> {
                // Toast.makeText(GroupBookingActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnCreateGroupBooking.setEnabled(true);
                btnCreateGroupBooking.setText("Tạo nhóm đặt vé");
            });
    }
}
