package com.FinalProject.feature_event_detail.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_event_detail.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_SCHEDULE = "extra_event_schedule";
    public static final String EXTRA_EVENT_LOCATION = "extra_event_location";
    public static final String EXTRA_EVENT_DESCRIPTION = "extra_event_description";
    public static final String EXTRA_GENERAL_PRICE = "extra_general_price";
    public static final String EXTRA_VIP_PRICE = "extra_vip_price";
    public static final String EXTRA_TAGS = "extra_tags";
    public static final String EXTRA_TIMELINE = "extra_timeline";

    private TextView tvEventTitle;
    private TextView tvEventSchedule;
    private TextView tvEventLocation;
    private TextView tvGeneralPrice;
    private TextView tvVipPrice;
    private TextView tvEventDescription;
    private TextView tvVipBenefits;
    private Chip chipTagOne;
    private Chip chipTagTwo;
    private Chip chipTagThree;
    private MaterialButton btnChooseSeat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        bindViews();
        populateUiFromIntent(getIntent());
    }

    private void bindViews() {
        tvEventTitle = findViewById(R.id.tv_event_title);
        tvEventSchedule = findViewById(R.id.tv_event_schedule);
        tvEventLocation = findViewById(R.id.tv_event_location);
        tvGeneralPrice = findViewById(R.id.tv_general_ticket_price);
        tvVipPrice = findViewById(R.id.tv_vip_ticket_price);
        tvEventDescription = findViewById(R.id.tv_event_description);
        tvVipBenefits = findViewById(R.id.tv_vip_benefit_list);
        chipTagOne = findViewById(R.id.chip_tag_one);
        chipTagTwo = findViewById(R.id.chip_tag_two);
        chipTagThree = findViewById(R.id.chip_tag_three);
        btnChooseSeat = findViewById(R.id.btn_choose_seat);

        btnChooseSeat.setOnClickListener(v ->
                Toast.makeText(this, R.string.event_detail_cta, Toast.LENGTH_SHORT).show()
        );
    }

    private void populateUiFromIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_EVENT_TITLE);
        String schedule = intent.getStringExtra(EXTRA_EVENT_SCHEDULE);
        String location = intent.getStringExtra(EXTRA_EVENT_LOCATION);
        String description = intent.getStringExtra(EXTRA_EVENT_DESCRIPTION);
        String generalPrice = intent.getStringExtra(EXTRA_GENERAL_PRICE);
        String vipPrice = intent.getStringExtra(EXTRA_VIP_PRICE);
        String benefits = intent.getStringExtra("extra_vip_benefits");

        ArrayList<String> tags = intent.getStringArrayListExtra(EXTRA_TAGS);
        ArrayList<String> timelineRaw = intent.getStringArrayListExtra(EXTRA_TIMELINE);

        if (title != null) {
            tvEventTitle.setText(title);
        }
        if (schedule != null) {
            tvEventSchedule.setText(schedule);
        }
        if (location != null) {
            tvEventLocation.setText(location);
        }
        if (description != null) {
            tvEventDescription.setText(description);
        }
        if (generalPrice != null) {
            tvGeneralPrice.setText(generalPrice);
        }
        if (vipPrice != null) {
            tvVipPrice.setText(vipPrice);
        }
        if (benefits != null) {
            tvVipBenefits.setText(benefits);
        }

        applyTags(tags);
        applyTimeline(timelineRaw);
    }

    private void applyTags(@Nullable List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<Chip> chips = Arrays.asList(chipTagOne, chipTagTwo, chipTagThree);
        for (int i = 0; i < chips.size(); i++) {
            Chip chip = chips.get(i);
            if (chip == null) {
                continue;
            }
            chip.setVisibility(i < tags.size() ? android.view.View.VISIBLE : android.view.View.GONE);
            if (i < tags.size()) {
                chip.setText(tags.get(i));
            }
        }
    }

    private void applyTimeline(@Nullable List<String> timelineRaw) {
        if (timelineRaw == null || timelineRaw.isEmpty()) {
            return;
        }
        List<Integer> timelineViewIds = Arrays.asList(
                R.id.view_timeline_one,
                R.id.view_timeline_two,
                R.id.view_timeline_three
        );
        for (int i = 0; i < timelineViewIds.size(); i++) {
            int viewId = timelineViewIds.get(i);
            android.view.View timelineContainer = findViewById(viewId);
            if (timelineContainer == null) {
                continue;
            }
            if (i >= timelineRaw.size()) {
                timelineContainer.setVisibility(android.view.View.GONE);
                continue;
            }

            String item = timelineRaw.get(i);
            String[] parts = item.split("\\|");
            String time = parts.length > 0 ? parts[0].trim() : "";
            String desc = parts.length > 1 ? parts[1].trim() : "";

            TextView timeView = timelineContainer.findViewById(R.id.tv_timeline_time);
            TextView descView = timelineContainer.findViewById(R.id.tv_timeline_description);
            timeView.setText(time);
            descView.setText(desc);
        }
    }
}
