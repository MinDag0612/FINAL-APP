package com.FinalProject.feature_home.model;

import java.util.Collections;
import java.util.List;

public class HomeContent {
    private final HomeUser user;
    private final List<HomeEvent> events;
    private final List<HomeArtist> artists;
    private final RecentTicketInfo recentTicketInfo;

    public HomeContent(
            HomeUser user,
            List<HomeEvent> events,
            List<HomeArtist> artists,
            RecentTicketInfo recentTicketInfo
    ) {
        this.user = user;
        this.events = events != null ? events : Collections.emptyList();
        this.artists = artists != null ? artists : Collections.emptyList();
        this.recentTicketInfo = recentTicketInfo != null ? recentTicketInfo : RecentTicketInfo.empty();
    }

    public HomeUser getUser() {
        return user;
    }

    public List<HomeEvent> getEvents() {
        return events;
    }

    public List<HomeArtist> getArtists() {
        return artists;
    }

    public RecentTicketInfo getRecentTicketInfo() {
        return recentTicketInfo;
    }
}
