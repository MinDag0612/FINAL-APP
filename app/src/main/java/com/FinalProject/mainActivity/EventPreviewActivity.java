package com.FinalProject.mainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.FinalProject.feature_login.presentation.LoginActivity;
import com.google.android.material.button.MaterialButton;

public class EventPreviewActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_preview);

        // 1. Lấy thông tin từ Intent
        Intent intent = getIntent();
        eventId = intent.getStringExtra("EVENT_ID");
        eventName = intent.getStringExtra("EVENT_NAME"); // Lấy thêm tên sự kiện

        // Ánh xạ View
        TextView eventNameTextView = findViewById(R.id.tv_event_name_preview);
        MaterialButton goToLoginButton = findViewById(R.id.btn_go_to_login);

        // 2. Hiển thị thông tin
        if (eventName != null && !eventName.isEmpty()) {
            eventNameTextView.setText(eventName);
        } else {
            eventNameTextView.setText("Chi tiết sự kiện");
        }

        // 3. Gắn sự kiện click cho nút
        goToLoginButton.setOnClickListener(v -> {
            // Tạo intent để mở LoginActivity
            Intent loginIntent = new Intent(EventPreviewActivity.this, LoginActivity.class);

            // (Tùy chọn nâng cao) Gắn thông tin deep link vào để sau khi đăng nhập thành công
            // có thể quay lại đúng trang chi tiết sự kiện.
            loginIntent.putExtra("DEEP_LINK_EVENT_ID", eventId);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(loginIntent);
        });
    }
}
