package com.FinalProject.feature_create_event.presentation;

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

import com.FinalProject.feature_create_event.R;
import com.FinalProject.feature_create_event.domain.CreateEventUseCase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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
    TextInputEditText etTicketPrice;
    TextInputEditText etTicketQuantity;
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

    private void init() {
        etStartTime = findViewById(R.id.et_event_start_time);
        etEndTime = findViewById(R.id.et_event_end_time);
        btnBack = findViewById(R.id.btn_back);
        eventType = findViewById(R.id.actv_event_type);

        etEventName = findViewById(R.id.et_event_name);
        etEventDescription = findViewById(R.id.et_event_description);
        etEventLocation = findViewById(R.id.et_event_location);
        etEventCast = findViewById(R.id.et_event_cast);
        etTicketPrice = findViewById(R.id.et_ticket_price);
        etTicketQuantity = findViewById(R.id.et_ticket_quantity);
        submitBtn = findViewById(R.id.btn_create_event);
    }

    private void setEtStartEndTime() {
        etStartTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (dateView, y, m, d) -> {
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

            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (dateView, y, m, d) -> {
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

    private void setBtnBack() {
        btnBack.setOnClickListener(v -> onBackPressed());
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

    private void setSubmitBtn() {
        submitBtn.setOnClickListener(v -> {
            String eventName = etEventName.getText().toString().trim();
            String eventDescription = etEventDescription.getText().toString().trim();
            String eventStart = etStartTime.getText().toString().trim();
            String eventEnd = etEndTime.getText().toString().trim();
            String eventLocation = etEventLocation.getText().toString().trim();
            String eventCast = etEventCast.getText().toString().trim();
            String eventTypeStr = eventType.getText().toString().trim();
            String priceStr = etTicketPrice.getText().toString().trim();
            String quantityStr = etTicketQuantity.getText().toString().trim();
            String uid = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                    .getString("UID", null);

            if (eventName.isEmpty() || eventDescription.isEmpty() || eventStart.isEmpty() || eventEnd.isEmpty() || eventLocation.isEmpty()
                    || priceStr.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (eventTypeStr.isEmpty()) {
                eventTypeStr = "Other";
            }

            int ticketPrice;
            int ticketQuantity;
            try {
                ticketPrice = Integer.parseInt(priceStr);
                ticketQuantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá vé và số lượng phải là số", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ticketPrice <= 0 || ticketQuantity <= 0) {
                Toast.makeText(this, "Giá vé và số lượng phải > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            Events newEvent = new Events(
                    eventName,
                    eventDescription,
                    eventStart,
                    eventEnd,
                    eventCast,
                    eventLocation,
                    eventTypeStr,
                    uid,
                    ticketPrice
            );

            Log.d("CreateEventActivity", "setSubmitBtn: " + newEvent.toString());

            com.FinalProject.core.model.TicketInfor defaultTicket = new com.FinalProject.core.model.TicketInfor(
                    "Vé chuẩn",
                    ticketQuantity,
                    0,
                    ticketPrice
            );

            createEventUseCase.execute(newEvent, defaultTicket, new CreateEventUseCase.CreateEventCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CreateEventActivity.this, "Tạo sự kiện thành công", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    Log.d("CreateEventActivity", "onError: " + message);
                    Toast.makeText(CreateEventActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
