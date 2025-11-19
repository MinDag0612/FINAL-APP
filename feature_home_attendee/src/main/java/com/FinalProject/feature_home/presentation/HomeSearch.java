package com.FinalProject.feature_home.presentation;

import android.text.Editable;
import android.text.TextWatcher;

import com.FinalProject.feature_home.model.HomeEvent;
import com.FinalProject.feature_home.presentation.adapter.HomeEventAdapter;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeSearch {
    private final TextInputEditText searchEditText;
    private final List<HomeEvent> allEvents;
    private final HomeEventAdapter eventAdapter;

    public HomeSearch(TextInputEditText searchEditText, List<HomeEvent> allEvents, HomeEventAdapter eventAdapter) {
        this.searchEditText = searchEditText;
        this.allEvents = allEvents;
        this.eventAdapter = eventAdapter;
    }

    public void setupSearchListener() {
        this.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvent(s.toString());
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEvent(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // nếu ô tìm kiếm trống, hiển thị tất cả
            eventAdapter.submitList(new ArrayList<>(allEvents));
            return;
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        List<HomeEvent> filtered = new ArrayList<>();
        for (HomeEvent event : allEvents) {
            if (event.getName().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                    || event.getLocation().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                    || (event.getEventType() != null && event.getEventType().toLowerCase(Locale.ROOT).contains(lowerKeyword))
                    || (event.getCast() != null && event.getCast().toLowerCase(Locale.ROOT).contains(lowerKeyword))) {
                filtered.add(event);
            }
        }

        eventAdapter.submitList(filtered);
    }

    public TextInputEditText getSearchEditText() {
        return searchEditText;
    }

}
