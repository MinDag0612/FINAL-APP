package com.FinalProject.feature_event_detail.model;

/**
 * Một mốc thời gian trong lịch trình sự kiện.
 */
public class TimelineItem {
    private final String time;
    private final String title;
    private final String description;

    public TimelineItem(String time, String title, String description) {
        this.time = time;
        this.title = title;
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
