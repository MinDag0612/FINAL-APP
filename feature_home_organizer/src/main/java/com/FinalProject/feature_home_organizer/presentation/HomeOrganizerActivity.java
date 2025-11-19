package com.FinalProject.feature_home_organizer.presentation;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_home_organizer.R;
import com.google.android.material.button.MaterialButton;

public class HomeOrganizerActivity extends AppCompatActivity  {

    MaterialButton createEventBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_organizer);

        init();
        setCreateEventBtn();
    }

    private void init(){
        createEventBtn = findViewById(R.id.btn_create_event);
    }

    private void setCreateEventBtn(){
        createEventBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}