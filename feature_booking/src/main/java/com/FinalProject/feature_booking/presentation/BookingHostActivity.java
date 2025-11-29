package com.FinalProject.feature_booking.presentation;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.FinalProject.feature_booking.R;

public class BookingHostActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_host);

        Intent it = getIntent();
        String eventId   = it.getStringExtra("eventId");
        String showId    = it.getStringExtra("showId");
        String eventTitle= it.getStringExtra("eventTitle");

        NavHostFragment nh = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host);
        if (nh != null && savedInstanceState == null) {
            NavController nav = nh.getNavController();
            Bundle args = new Bundle();
            args.putString("eventId", eventId == null ? "" : eventId);
            args.putString("showId",  showId   == null ? "" : showId);
            args.putString("eventTitle", eventTitle == null ? "" : eventTitle);
            nav.setGraph(R.navigation.nav_booking, args);
        }
    }
}
