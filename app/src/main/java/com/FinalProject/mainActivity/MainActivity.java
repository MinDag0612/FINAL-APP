package com.FinalProject.mainActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketItem;
import com.FinalProject.core.util.HandleNotification;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.feature_event_detail.presentation.EventDetailActivity;
import com.FinalProject.feature_login.presentation.LoginActivity;
import com.FinalProject.core.util.Seeder;
import com.FinalProject.feature_booking.presentation.BookingHostActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔹 DEV: seed sample event + tickets_infor (an toàn, id cố định + merge)
        Seeder.runSeed();

        Intent intent = getIntent();
        Uri data = intent.getData();

        // Kiểm tra xem có phải là deep link không
        if (data != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Log.d("DEEPLINK", "Deep Link data received: " + data.toString());
            List<String> segments = data.getPathSegments();

            // Giả sử deep link có dạng "yourapp://event/{eventId}/{eventName}"
            if (segments.size() >= 2 && segments.get(0).equals("event")) {
                String eventId = segments.get(1); // Lấy eventId
                Log.d("DEEPLINK", "Event ID from Deep Link: " + eventId);

                // Tạo Intent để mở EventDetailActivity
                Intent detailIntent = new Intent(this, EventDetailActivity.class);
                detailIntent.putExtra("EVENT_ID", eventId);
                startActivity(detailIntent);
                finish();
                return; // Ngừng thực thi các lệnh khác
            }
        }

        // Nếu không phải deep link, chạy luồng bình thường
        // (Trong trường hợp này, bạn có thể chuyển đến màn hình Login)
        startActivity(new Intent(this, LoginActivity.class));
        finish();
//        HandleNotification.test(this);
    }


}
