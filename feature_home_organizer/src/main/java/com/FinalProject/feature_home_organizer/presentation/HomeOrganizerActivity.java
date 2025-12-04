package com.FinalProject.feature_home_organizer.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_attendee_manager_organizer.presentation.AttendeeListActivity;
import com.FinalProject.feature_create_event.presentation.CreateEventActivity;
import com.FinalProject.feature_home_organizer.R;
import com.FinalProject.feature_home_organizer.data.OrganizerEventRepository;
import com.FinalProject.feature_home_organizer.domain.GetOrganizerEventsUseCase;
import com.FinalProject.feature_profile.presentation.ProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeOrganizerActivity extends AppCompatActivity  {
    private MaterialButton createEventBtn;
    private MaterialButton quickCreateEventBtn;
    private RecyclerView rvEvents;
    private CircularProgressIndicator progressIndicator;
    private TextView tvEmpty;
    private TextView tvActiveEventsCount;
    private OrganizerEventAdapter adapter;
    private final GetOrganizerEventsUseCase getEventsUseCase = new GetOrganizerEventsUseCase();
    private FrameLayout btnAvt;
    private final List<OrganizerEventRepository.EventItem> currentEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_organizer);

        init();
        setCreateEventBtn();
        setBtnAvt();
    }

    private void init(){
        createEventBtn = findViewById(R.id.btn_create_event);
        quickCreateEventBtn = findViewById(R.id.btn_quick_create_event);
        rvEvents = findViewById(R.id.rv_active_events);
        progressIndicator = findViewById(R.id.progress_events);
        tvEmpty = findViewById(R.id.tv_empty_events);
        tvActiveEventsCount = findViewById(R.id.tv_active_events_count);
        btnAvt = findViewById(R.id.btn_avt_organize);
        adapter = new OrganizerEventAdapter(new OrganizerEventAdapter.Listener() {
            @Override
            public void onEdit(String eventId) {
                openCreateEvent(eventId);
            }

            @Override
            public void onCheckin(String eventId) {
                Snackbar.make(rvEvents, "QR Check-in sẽ được nối vào màn check-in thực tế", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onAttendeeList(String eventId) {
                openAttendeeList(eventId);
            }

            @Override
            public void onTicketManager(String eventId, String eventName) {
                openTicketManager(eventId, eventName);
            }

            @Override
            public void onSalesReport(String eventId, String eventName) {
                openSalesReport(eventId, eventName);
            }
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void setBtnAvt(){
        android.util.Log.d("HomeOrganizerActivity", "setBtnAvt called, btnAvt is " + (btnAvt != null ? "not null" : "null"));
        if (btnAvt != null) {
            btnAvt.setOnClickListener(v -> {
                android.util.Log.d("HomeOrganizerActivity", "Avatar button clicked");
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        } else {
            android.util.Log.e("HomeOrganizerActivity", "btnAvt is null, click listener not set");
        }
    }

    private void setCreateEventBtn(){
        if (createEventBtn != null) {
            createEventBtn.setOnClickListener(v -> openCreateEvent(null));
        }
        if (quickCreateEventBtn != null) {
            quickCreateEventBtn.setOnClickListener(v -> openCreateEvent(null));
        }
    }

    private void openCreateEvent(String eventId) {
        Intent intent = new Intent(this, CreateEventActivity.class);
        if (eventId != null) {
            intent.putExtra("EXTRA_EVENT_ID", eventId);
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        String uid = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("UID", null);
        android.util.Log.d("HomeOrganizerActivity", "loadEvents - Retrieved UID from SharedPreferences: " + uid);
        
        // Debug Toast - hiển thị UID
        if (uid != null && rvEvents != null) {
            // android.widget.Toast.makeText(this, "Đang tìm sự kiện cho UID: " + uid, android.widget.Toast.LENGTH_LONG).show();
        }
        
        if (uid == null) {
            android.util.Log.e("HomeOrganizerActivity", "UID is null, cannot load events");
            if (rvEvents != null) {
                Snackbar.make(rvEvents, "Không tìm thấy UID organizer", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.VISIBLE);
        getEventsUseCase.execute(uid, new GetOrganizerEventsUseCase.Callback() {
            @Override
            public void onSuccess(List<OrganizerEventRepository.EventItem> events) {
                if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                android.util.Log.d("HomeOrganizerActivity", "onSuccess - Received events count: " + (events != null ? events.size() : "null"));
                
                // Debug Toast
                if (rvEvents != null) {
                    // android.widget.Toast.makeText(HomeOrganizerActivity.this, "Load được " + (events != null ? events.size() : 0) + " sự kiện", android.widget.Toast.LENGTH_LONG).show();
                }
                
                currentEvents.clear();
                if (events != null) currentEvents.addAll(events);
                adapter.submitList(events);
                
                // Update KPI counter
                int eventCount = events != null ? events.size() : 0;
                if (tvActiveEventsCount != null) {
                    tvActiveEventsCount.setText(eventCount + " sự kiện đang chạy");
                }
                
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(events == null || events.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(String message) {
                if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                if (rvEvents != null) {
                    Snackbar.make(rvEvents, message != null ? message : "Lỗi tải sự kiện", Snackbar.LENGTH_SHORT).show();
                }
                if (tvEmpty != null) tvEmpty.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    private void openAttendeeList(String eventId) {
        String name = "";
        for (OrganizerEventRepository.EventItem item : currentEvents) {
            if (item.id.equals(eventId)) {
                name = item.name;
                break;
            }
        }
        Intent intent = new Intent(this, AttendeeListActivity.class);
        intent.putExtra(AttendeeListActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(AttendeeListActivity.EXTRA_EVENT_NAME, name);
        startActivity(intent);
    }

    private void openTicketManager(String eventId, String eventName) {
        try {
            Class<?> ticketManagerClass = Class.forName("com.FinalProject.feature_ticket_manager.presentation.TicketManagerActivity");
            Intent intent = new Intent(this, ticketManagerClass);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_name", eventName);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Snackbar.make(rvEvents, "Không tìm thấy module Quản lý vé", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void openSalesReport(String eventId, String eventName) {
        try {
            Class<?> salesReportClass = Class.forName("com.FinalProject.feature_sales_report.presentation.SalesReportActivity");
            Intent intent = new Intent(this, salesReportClass);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_name", eventName);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Snackbar.make(rvEvents, "Không tìm thấy module Báo cáo bán vé", Snackbar.LENGTH_SHORT).show();
        }
    }
}
