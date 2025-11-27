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

import com.FinalProject.core.model.Orders;
import com.FinalProject.core.util.HandleNotification;
import com.FinalProject.feature_create_event.R;
import com.FinalProject.feature_create_event.data.CreateEventRepositoryImpl;
import com.FinalProject.feature_create_event.domain.CreateEventUseCase;
import com.FinalProject.feature_create_event.domain.LoadEventForEditUseCase;
import com.FinalProject.feature_create_event.domain.UpdateEventUseCase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.FinalProject.core.model.Events;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


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


    String eventId = null;
    boolean isEditMode = false;

    CreateEventUseCase createEventUseCase = new CreateEventUseCase();
    UpdateEventUseCase updateEventUseCase = new UpdateEventUseCase();
    LoadEventForEditUseCase loadEventForEditUseCase = new LoadEventForEditUseCase();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        eventId = getIntent().getStringExtra("EXTRA_EVENT_ID");
        isEditMode = eventId != null && !eventId.isEmpty();

        init();
        setEtStartEndTime();
        setBtnBack();
        setEventType();
        if (isEditMode) {
            submitBtn.setText("Lưu thay đổi");
            loadEventData();
        }
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

            if (!isEditMode) {
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
            } else {
                updateEventUseCase.execute(eventId, newEvent, defaultTicket, new UpdateEventUseCase.Callback() {
                    @Override
                    public void onSuccess() {
                        CreateEventRepositoryImpl.getFcmByEventId(eventId).addOnSuccessListener(fcmList -> {
                            SendNotificationEvent.sendUpdateNoti(CreateEventActivity.this, fcmList, newEvent);
                        });

                        Toast.makeText(CreateEventActivity.this, "Lưu sự kiện thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.d("CreateEventActivity", "update error: " + message);
                        Toast.makeText(CreateEventActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadEventData() {
        loadEventForEditUseCase.execute(eventId, new LoadEventForEditUseCase.Callback() {
            @Override
            public void onSuccess(com.FinalProject.feature_create_event.data.EventEditorRepository.EventWithTicket data) {
                Events e = data.event;
                if (e != null) {
                    etEventName.setText(e.getEvent_name());
                    etEventDescription.setText(e.getEvent_descrip());
                    etStartTime.setText(e.getEvent_start());
                    etEndTime.setText(e.getEvent_end());
                    etEventLocation.setText(e.getLocation());
                    etEventCast.setText(e.getCast());
                    eventType.setText(e.getEvent_type(), false);
                    if (e.getBase_price() > 0) {
                        etTicketPrice.setText(String.valueOf(e.getBase_price()));
                    }
                }
                if (data.ticket != null) {
                    if (data.ticket.getTickets_price() > 0) {
                        etTicketPrice.setText(String.valueOf(data.ticket.getTickets_price()));
                    }
                    if (data.ticket.getTickets_quantity() > 0) {
                        etTicketQuantity.setText(String.valueOf(data.ticket.getTickets_quantity()));
                    }
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(CreateEventActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
