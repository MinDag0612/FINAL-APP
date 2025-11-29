package com.FinalProject.mainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketItem;
import com.FinalProject.core.util.HandleNotification;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.feature_login.presentation.LoginActivity;
import com.FinalProject.core.util.Seeder;
import com.FinalProject.feature_booking.presentation.BookingHostActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔹 DEV: seed sample event + tickets_infor (an toàn, id cố định + merge)
        Seeder.runSeed();

        // 🔹 Dùng đúng eventId mà Seeder tạo
        Intent intent = new Intent(this, BookingHostActivity.class)
                .putExtra("eventId", "seed_tedxyouth_2024")
                .putExtra("eventTitle", "TEDxYouth Saigon 2024");

        startActivity(intent);
        finish();
//        HandleNotification.test(this);
    }
}




