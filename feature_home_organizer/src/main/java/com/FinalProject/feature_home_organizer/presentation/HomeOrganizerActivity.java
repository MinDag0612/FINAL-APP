package com.FinalProject.feature_home_organizer.presentation;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_create_event.presentation.CreateEventActivity;
import com.FinalProject.feature_attendee_manager_organizer.presentation.AttendeeListActivity;
import com.FinalProject.feature_home_organizer.R;
import com.FinalProject.feature_home_organizer.data.OrganizerEventRepository;
import com.FinalProject.feature_home_organizer.domain.GetOrganizerEventsUseCase;
import com.FinalProject.feature_profile.presentation.ProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import android.widget.FrameLayout;
import android.widget.TextView;

public class HomeOrganizerActivity extends AppCompatActivity  {

    TextView createEventBtn;
    MaterialButton quickCreateEventBtn;
    RecyclerView rvEvents;
    CircularProgressIndicator progressIndicator;
    TextView tvEmpty;
    OrganizerEventAdapter adapter;
    GetOrganizerEventsUseCase getEventsUseCase = new GetOrganizerEventsUseCase();
    FrameLayout btn_avt;
    java.util.List<OrganizerEventRepository.EventItem> currentEvents = new java.util.ArrayList<>();

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
        btn_avt = findViewById(R.id.btn_avt_organize);
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
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void setBtnAvt(){
        btn_avt.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setCreateEventBtn(){
        createEventBtn.setOnClickListener(v -> openCreateEvent(null));
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
        if (uid == null) {
            Snackbar.make(rvEvents, "Không tìm thấy UID organizer", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.VISIBLE);
        getEventsUseCase.execute(uid, new GetOrganizerEventsUseCase.Callback() {
            @Override
            public void onSuccess(java.util.List<OrganizerEventRepository.EventItem> events) {
                if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                currentEvents.clear();
                if (events != null) currentEvents.addAll(events);
                adapter.submitList(events);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(events == null || events.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(String message) {
                if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                Snackbar.make(rvEvents, message != null ? message : "Lỗi tải sự kiện", Snackbar.LENGTH_SHORT).show();
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
}
