package com.FinalProject.feature_home_organizer.presentation;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.FinalProject.feature_home_organizer.R;
import com.FinalProject.feature_home_organizer.domain.CreateEventUseCase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.FinalProject.core.model.Events;

import java.util.ArrayList;
import java.util.Calendar;


public class CreateEventActivity extends AppCompatActivity {

    TextInputEditText etStartTime;
    TextInputEditText etEndTime;
    AutoCompleteTextView eventType;
    TextInputEditText etEventName;
    TextInputEditText etEventDescription;
    TextInputEditText etEventLocation;
    TextInputEditText etEventCast;

    MaterialButton submitBtn;

    ImageButton btnBack;

    CreateEventUseCase createEventUseCase = new CreateEventUseCase();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        init();
        setEtStartEndTime();
        setBtnBack();
        setEventType();
        setSubmitBtn();

    }

    private void init(){
        etStartTime = findViewById(R.id.et_event_start_time);
        etEndTime = findViewById(R.id.et_event_end_time);
        btnBack = findViewById(R.id.btn_back);
        eventType = findViewById(R.id.actv_event_type);

        etEventName = findViewById(R.id.et_event_name);
        etEventDescription = findViewById(R.id.et_event_description);
        etEventLocation = findViewById(R.id.et_event_location);
        etEventCast = findViewById(R.id.et_event_cast);
        submitBtn = findViewById(R.id.btn_create_event);
    }

    private void setEtStartEndTime(){
        etStartTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // 👉 Date Picker
            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (dateView, y, m, d) -> {
                        // Sau khi chọn ngày → bật TimePicker
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);

                        TimePickerDialog timePicker = new TimePickerDialog(
                                this,
                                (timeView, h, min) -> {
                                    String result = String.format(
                                            "%02d/%02d/%04d %02d:%02d",
                                            d, (m + 1), y, h, min
                                    );
                                    etStartTime.setText(result);
                                },
                                hour,
                                minute,
                                true
                        );
                        timePicker.show();
                    },
                    year,
                    month,
                    day
            );

            datePicker.show();
        });

        etEndTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // 👉 Date Picker
            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (dateView, y, m, d) -> {
                        // Sau khi chọn ngày → bật TimePicker
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);

                        TimePickerDialog timePicker = new TimePickerDialog(
                                this,
                                (timeView, h, min) -> {
                                    String result = String.format(
                                            "%02d/%02d/%04d %02d:%02d",
                                            d, (m + 1), y, h, min
                                    );
                                    etEndTime.setText(result);
                                },
                                hour,
                                minute,
                                true
                        );
                        timePicker.show();
                    },
                    year,
                    month,
                    day
            );

            datePicker.show();
        });
    }

    private void setBtnBack(){
        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    public void setEventType() {
        ArrayList<String> typeList = Events.getEventType();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                typeList
        );

        eventType.setAdapter(adapter);
    }

    private void setSubmitBtn(){
        submitBtn.setOnClickListener(v -> {
            String eventName = etEventName.getText().toString().trim();
            String eventDescription = etEventDescription.getText().toString().trim();
            String eventStart = etStartTime.getText().toString().trim();
            String eventEnd = etEndTime.getText().toString().trim();
            String eventLocation = etEventLocation.getText().toString().trim();
            String eventCast = etEventCast.getText().toString().trim();

            String eventTypeStr = eventType.getText().toString().trim();
            String uid = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                    .getString("UID", null);

            if (eventName.isEmpty() || eventDescription.isEmpty() || eventStart.isEmpty() || eventEnd.isEmpty() || eventLocation.isEmpty()){
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Events newEvent = new Events(
                    eventName,
                    eventDescription,
                    eventStart,
                    eventEnd,
                    eventLocation,
                    eventCast,
                    eventTypeStr,
                    uid
            );

            Log.d("CreateEventActivity", "setSubmitBtn: " + newEvent.toString());

            createEventUseCase.excute(newEvent, new CreateEventUseCase.CreateEventCallback(){
                @Override
                public void onSuccess() {
                    finish();
                    Toast.makeText(CreateEventActivity.this, "Create event successful", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String message) {
                    Log.d("CreateEventActivity", "onError: " + message);
                }

            });
            finish();
        });
    }
}