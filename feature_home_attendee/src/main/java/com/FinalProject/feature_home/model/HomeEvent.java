package com.FinalProject.feature_home.model;

public class HomeEvent {
    private final String id;
    private final String name;
    private final String description;
    private final String location;
    private final String eventType;
    private final String startTimeIso;
    private final String cast;
    private long startingPrice;

    public HomeEvent(
            String id,
            String name,
            String description,
            String location,
            String eventType,
            String startTimeIso,
            String cast
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.eventType = eventType;
        this.startTimeIso = startTimeIso;
        this.cast = cast;
        this.startingPrice = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStartTimeIso() {
        return startTimeIso;
    }

    public String getCast() {
        return cast;
    }

    public long getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(long startingPrice) {
        this.startingPrice = startingPrice;
    }
}
