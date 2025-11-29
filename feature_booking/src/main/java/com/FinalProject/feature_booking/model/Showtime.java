package com.FinalProject.feature_booking.model;

public class Showtime {
    private String showId;
    private String date;    // yyyy-MM-dd
    private String time;    // HH:mm
    private Long capacity;

    public Showtime() {}

    public Showtime(String showId, String date, String time, Long capacity) {
        this.showId = showId; this.date = date; this.time = time; this.capacity = capacity;
    }

    public String getShowId() { return showId; }
    public void setShowId(String showId) { this.showId = showId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Long getCapacity() { return capacity; }
    public void setCapacity(Long capacity) { this.capacity = capacity; }

    public String getLabel() { return (date == null? "" : date) + " • " + (time==null? "" : time); }
}
