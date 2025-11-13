package com.FinalProject.feature_event_detail.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable UI model mô tả toàn bộ thông tin chi tiết của một sự kiện.
 */
public class EventDetail {

    private final String id;
    private final String name;
    private final String description;
    private final String location;
    private final String eventType;
    private final String startTimeIso;
    private final String endTimeIso;
    private final String cast;
    private final String coverImage;
    private final List<String> tags;
    private final List<TicketTier> ticketTiers;
    private final List<TimelineItem> timelineItems;
    private final List<ReviewDisplayItem> reviews;
    private final double averageRating;
    private final int reviewCount;

    public EventDetail(
            @NonNull String id,
            @NonNull String name,
            @Nullable String description,
            @Nullable String location,
            @Nullable String eventType,
            @Nullable String startTimeIso,
            @Nullable String endTimeIso,
            @Nullable String cast,
            @Nullable String coverImage,
            @Nullable List<String> tags,
            @Nullable List<TicketTier> ticketTiers,
            @Nullable List<TimelineItem> timelineItems,
            @Nullable List<ReviewDisplayItem> reviews,
            double averageRating,
            int reviewCount
    ) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.location = location != null ? location : "";
        this.eventType = eventType != null ? eventType : "";
        this.startTimeIso = startTimeIso;
        this.endTimeIso = endTimeIso;
        this.cast = cast;
        this.coverImage = coverImage;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.ticketTiers = ticketTiers != null ? new ArrayList<>(ticketTiers) : new ArrayList<>();
        this.timelineItems = timelineItems != null ? new ArrayList<>(timelineItems) : new ArrayList<>();
        this.reviews = reviews != null ? new ArrayList<>(reviews) : new ArrayList<>();
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getLocation() {
        return location;
    }

    @NonNull
    public String getEventType() {
        return eventType;
    }

    @Nullable
    public String getStartTimeIso() {
        return startTimeIso;
    }

    @Nullable
    public String getEndTimeIso() {
        return endTimeIso;
    }

    @Nullable
    public String getCast() {
        return cast;
    }

    @Nullable
    public String getCoverImage() {
        return coverImage;
    }

    @NonNull
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @NonNull
    public List<TicketTier> getTicketTiers() {
        return Collections.unmodifiableList(ticketTiers);
    }

    @NonNull
    public List<TimelineItem> getTimelineItems() {
        return Collections.unmodifiableList(timelineItems);
    }

    @NonNull
    public List<ReviewDisplayItem> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }
}
