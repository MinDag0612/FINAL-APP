package com.FinalProject.feature_home.presentation;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_home.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupCategoryChips();
        setupActions();
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_categories);
        if (chipGroup == null || chipGroup.getChildCount() == 0) {
            return;
        }

        Chip firstChip = (Chip) chipGroup.getChildAt(0);
        if (firstChip != null) {
            firstChip.setChecked(true);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            TextView subtitle = findViewById(R.id.tv_suggestion_subtitle);
            if (subtitle == null) {
                return;
            }
            int selected = checkedIds.size();
            if (selected == 0) {
                subtitle.setText(R.string.home_subtitle_generic);
            } else {
                subtitle.setText(getString(R.string.home_subtitle_filtered, selected));
            }
        });
    }

    private void setupActions() {
        TextView viewAllEvents = findViewById(R.id.tv_view_all_events);
        if (viewAllEvents != null) {
            viewAllEvents.setOnClickListener(v ->
                    Toast.makeText(this, R.string.home_action_view_all, Toast.LENGTH_SHORT).show());
        }

        MaterialButton viewTicketButton = findViewById(R.id.btn_view_ticket);
        if (viewTicketButton != null) {
            viewTicketButton.setOnClickListener(v ->
                    Toast.makeText(this, R.string.home_action_view_ticket, Toast.LENGTH_SHORT).show());
        }
    }
}
