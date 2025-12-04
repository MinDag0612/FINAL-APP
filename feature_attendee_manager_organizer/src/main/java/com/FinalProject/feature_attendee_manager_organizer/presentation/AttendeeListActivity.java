package com.FinalProject.feature_attendee_manager_organizer.presentation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_attendee_manager_organizer.R;
import com.FinalProject.feature_attendee_manager_organizer.data.AttendeeRepository;
import com.FinalProject.feature_attendee_manager_organizer.domain.ExportAttendeesUseCase;
import com.FinalProject.feature_attendee_manager_organizer.domain.GetAttendeesUseCase;
import com.FinalProject.feature_attendee_manager_organizer.domain.SendBroadcastUseCase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.List;

public class AttendeeListActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    public static final String EXTRA_EVENT_NAME = "EXTRA_EVENT_NAME";

    private RecyclerView rv;
    private CircularProgressIndicator progress;
    private TextView tvEmpty;
    private AttendeeAdapter adapter;
    private final GetAttendeesUseCase getAttendeesUseCase = new GetAttendeesUseCase();
    private final ExportAttendeesUseCase exportUseCase = new ExportAttendeesUseCase();
    private final SendBroadcastUseCase sendBroadcastUseCase = new SendBroadcastUseCase();
    private String eventId;
    private String eventName;

    private final List<AttendeeRepository.AttendeeItem> adapterItemsHolder = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendee_list);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        rv = findViewById(R.id.rv_attendees);
        progress = findViewById(R.id.progress_attendees);
        tvEmpty = findViewById(R.id.tv_empty_attendees);
        MaterialButton btnCsv = findViewById(R.id.btn_export_csv);
        MaterialButton btnPdf = findViewById(R.id.btn_export_pdf);
        MaterialButton btnBroadcast = findViewById(R.id.btn_send_broadcast);
        MaterialButton btnMock = findViewById(R.id.btn_mock_data);

        if (tvTitle != null && eventName != null) {
            tvTitle.setText("Khách tham dự - " + eventName);
        }

        btnBack.setOnClickListener(v -> onBackPressed());

        adapter = new AttendeeAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnCsv.setOnClickListener(v -> exportCsv());
        btnPdf.setOnClickListener(v -> exportPdf());
        if (btnBroadcast != null) btnBroadcast.setOnClickListener(v -> promptBroadcast());
        if (btnMock != null) btnMock.setOnClickListener(v -> showMockData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendees();
    }

    private void loadAttendees() {
        if (progress != null) progress.setVisibility(android.view.View.VISIBLE);
        getAttendeesUseCase.execute(eventId, new GetAttendeesUseCase.Callback() {
            @Override
            public void onSuccess(List<AttendeeRepository.AttendeeItem> attendees) {
                if (progress != null) progress.setVisibility(android.view.View.GONE);
                setAdapterItems(attendees);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(attendees == null || attendees.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(String message) {
                if (progress != null) progress.setVisibility(android.view.View.GONE);
                Snackbar.make(rv, message != null ? message : "Lỗi tải danh sách", Snackbar.LENGTH_SHORT).show();
                if (tvEmpty != null) tvEmpty.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    private void exportCsv() {
        exportUseCase.exportCsv(this, adapterItems(), new ExportAttendeesUseCase.Callback() {
            @Override
            public void onSuccess(File file, String publicPath) {
                String pathMsg = publicPath != null && !publicPath.isEmpty()
                        ? "Đã lưu: " + publicPath
                        : "Đã lưu: " + file.getAbsolutePath();
                // Toast.makeText(AttendeeListActivity.this, pathMsg, Toast.LENGTH_SHORT).show();
                shareFile(file, "text/csv");
            }

            @Override
            public void onFailure(String message) {
                // Toast.makeText(AttendeeListActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportPdf() {
        exportUseCase.exportPdf(this, adapterItems(), new ExportAttendeesUseCase.Callback() {
            @Override
            public void onSuccess(File file, String publicPath) {
                String pathMsg = publicPath != null && !publicPath.isEmpty()
                        ? "Đã lưu: " + publicPath
                        : "Đã lưu: " + file.getAbsolutePath();
                // Toast.makeText(AttendeeListActivity.this, pathMsg, Toast.LENGTH_SHORT).show();
                shareFile(file, "application/pdf");
            }

            @Override
            public void onFailure(String message) {
                // Toast.makeText(AttendeeListActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<AttendeeRepository.AttendeeItem> adapterItems() {
        return adapterItemsHolder;
    }

    private void setAdapterItems(List<AttendeeRepository.AttendeeItem> items) {
        adapterItemsHolder.clear();
        if (items != null) adapterItemsHolder.addAll(items);
        adapter.submit(items);
    }

    private void shareFile(File file, String mime) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), mime);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, "Mở tệp với"));
    }

    private void showMockData() {
        List<AttendeeRepository.AttendeeItem> mock = new java.util.ArrayList<>();
        mock.add(new AttendeeRepository.AttendeeItem("mock1", "Nguyễn Văn A", "mock1@example.com", 3, 1200000));
        mock.add(new AttendeeRepository.AttendeeItem("mock2", "Trần Thị B", "mock2@example.com", 2, 800000));
        mock.add(new AttendeeRepository.AttendeeItem("mock3", "Lê C", "mock3@example.com", 1, 400000));
        setAdapterItems(mock);
        // Toast.makeText(this, "Đang hiển thị dữ liệu mẫu (không lưu Firestore)", Toast.LENGTH_SHORT).show();
        if (tvEmpty != null) tvEmpty.setVisibility(android.view.View.GONE);
    }

    private void promptBroadcast() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View view = inflater.inflate(R.layout.dialog_broadcast, null);
        TextInputLayout tilTitle = view.findViewById(R.id.til_broadcast_title);
        TextInputLayout tilContent = view.findViewById(R.id.til_broadcast_content);
        TextInputEditText etTitle = view.findViewById(R.id.et_broadcast_title);
        TextInputEditText etContent = view.findViewById(R.id.et_broadcast_content);

        builder.setTitle("Gửi thông báo")
                .setView(view)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent != null ? etContent.getText().toString().trim() : "";
                    sendBroadcast(title, content);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendBroadcast(String title, String content) {
        if (adapterItemsHolder.isEmpty()) {
            // Toast.makeText(this, "Chưa có danh sách khách để gửi", Toast.LENGTH_SHORT).show();
            return;
        }
        sendBroadcastUseCase.execute(eventId, eventName, title, content, adapterItemsHolder, new SendBroadcastUseCase.Callback() {
            @Override
            public void onSuccess() {
                // Toast.makeText(AttendeeListActivity.this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String message) {
                // Toast.makeText(AttendeeListActivity.this, message != null ? message : "Gửi thông báo thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
