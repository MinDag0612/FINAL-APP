package com.FinalProject.feature_home.model;

public class HomeArtist {
    private final String name;
    private final int eventCount;

    public HomeArtist(String name, int eventCount) {
        this.name = name;
        this.eventCount = eventCount;
    }

    public String getName() {
        return name;
    }

    public int getEventCount() {
        return eventCount;
    }
}
