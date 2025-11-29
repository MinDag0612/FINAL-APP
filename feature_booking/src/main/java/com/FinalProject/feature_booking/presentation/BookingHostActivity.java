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
        
        android.util.Log.d("BookingHostActivity", "onCreate - eventId: " + eventId + ", showId: " + showId + ", title: " + eventTitle);

        NavHostFragment nh = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host);
        if (nh != null && savedInstanceState == null) {
            NavController nav = nh.getNavController();
            
            // Tạo bundle với arguments
            Bundle args = new Bundle();
            args.putString("eventId", eventId != null ? eventId : "");
            args.putString("showId", showId != null ? showId : "");
            args.putString("eventTitle", eventTitle != null ? eventTitle : "");
            
            android.util.Log.d("BookingHostActivity", "Setting graph with startDestinationArgs:");
            android.util.Log.d("BookingHostActivity", "  -> Bundle eventId: '" + args.getString("eventId") + "'");
            android.util.Log.d("BookingHostActivity", "  -> Bundle showId: '" + args.getString("showId") + "'");
            android.util.Log.d("BookingHostActivity", "  -> Bundle eventTitle: '" + args.getString("eventTitle") + "'");
            
            // Inflate graph và set start destination arguments
            androidx.navigation.NavInflater inflater = nav.getNavInflater();
            androidx.navigation.NavGraph graph = inflater.inflate(R.navigation.nav_booking);
            graph.setStartDestination(R.id.eventDetailFragment);
            
            // Set graph với start destination args - đây là cách đúng
            nav.setGraph(graph, args);
            
            android.util.Log.d("BookingHostActivity", "Graph set with start destination args");
        }
    }
}
