package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.data.BookingRepository;
import com.FinalProject.feature_booking.model.Showtime;
import com.FinalProject.feature_booking.model.OrderResult;
import com.FinalProject.feature_booking.ui.ShowtimeAdapter;
import com.FinalProject.feature_booking.ui.TicketTypeAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.NumberFormat;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity
        implements ShowtimeAdapter.Listener, TicketTypeAdapter.Listener {

    private BookingViewModel vm;
    private String eventId;
    private String selectedDate;
    private String selectedShowId;

    private RecyclerView rvShowtimes, rvTicketTypes;
    private TextView tvTotal;
    private Button btnBook;
    private Button btnGroupBooking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check nếu được gọi từ Home với flag open_my_tickets
        boolean openMyTickets = getIntent().getBooleanExtra("open_my_tickets", false);
        if (openMyTickets) {
            // Navigate trực tiếp đến MyTicketsFragment
            setContentView(R.layout.activity_booking);
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new MyTicketsFragment())
                    .commit();
            }
            return;
        }
        
        setContentView(R.layout.activity_booking);

        // Lấy eventId từ Intent (fallback demo)
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            eventId = "DEMO_EVENT";
        }

        // Dùng BookingRepository (Firestore) thay cho BookingRepositoryImpl hard-code
        vm = new ViewModelProvider(
                this,
                new BookingViewModelFactory(BookingRepository.getInstance())
        ).get(BookingViewModel.class);

        rvShowtimes   = findViewById(R.id.rvShowtimes);
        rvTicketTypes = findViewById(R.id.rvTicketTypes);
        tvTotal       = findViewById(R.id.tvTotal);
        btnBook       = findViewById(R.id.btnBook);
        btnGroupBooking = findViewById(R.id.btnGroupBooking);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_booking);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvShowtimes.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );
        rvTicketTypes.setLayoutManager(new LinearLayoutManager(this));

        ShowtimeAdapter showtimeAdapter     = new ShowtimeAdapter(this);
        TicketTypeAdapter ticketTypeAdapter = new TicketTypeAdapter(this);
        rvShowtimes.setAdapter(showtimeAdapter);
        rvTicketTypes.setAdapter(ticketTypeAdapter);

        // Quan sát list ngày
        vm.dates.observe(this, dates -> {
            if (dates != null && !dates.isEmpty()) {
                selectedDate = dates.get(0);
                selectedShowId = null;
                vm.loadShowtimes(eventId, selectedDate);
            }
        });

        // Quan sát showtimes và ticket types
        vm.showtimes.observe(this, showtimes -> showtimeAdapter.submit(showtimes));
        vm.ticketTypes.observe(this, types -> ticketTypeAdapter.submit(types));

        // Quan sát tổng tiền
        vm.totalPrice.observe(this, total -> {
            long t = (total == null ? 0L : total);
            tvTotal.setText(
                    NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(t)
            );
            btnBook.setEnabled(t > 0);
        });

        // Quan sát error
        vm.error.observe(this, e -> {
            if (e != null && !e.isEmpty()) {
                // Toast.makeText(this, e, Toast.LENGTH_SHORT).show();
            }
        });

        // Quan sát kết quả đặt vé
        vm.orderResult.observe(this, r -> {
            if (r != null) {
                // Toast.makeText(this, "Đặt vé thành công: " + r.getOrderId(), Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // Nút đặt vé
        btnBook.setOnClickListener(v -> {
            if (selectedShowId == null) {
                // Toast.makeText(this, "Vui lòng chọn suất diễn.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Dùng FirebaseAuthHelper để lấy userId chuẩn
            String userId = FirebaseAuthHelper.getCurrentUserUid();
            if (userId == null || userId.trim().isEmpty()) {
                // Toast.makeText(this, "Bạn cần đăng nhập để đặt vé.", Toast.LENGTH_LONG).show();
                return;
            }
            vm.book(userId, eventId, selectedShowId);
        });

        // Nút đặt vé nhóm
        btnGroupBooking.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, GroupBookingActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Load ngày ban đầu
        vm.loadDates(eventId);
    }

    @Override
    public void onShowtimeClicked(Showtime s) {
        selectedShowId = s.getShowId();
        vm.loadTicketTypes(eventId, selectedShowId);
    }

    @Override
    public void onChangeQuantity(String typeId, long unitPrice, int delta) {
        vm.changeQuantity(typeId, unitPrice, delta);
    }
}
