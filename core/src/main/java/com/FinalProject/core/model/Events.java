package com.FinalProject.core.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Events {
    private String event_name = null;
    private String event_descrip = null;
    private String event_start = null;
    private String event_end = null;
    private String cast = null;
    private String location = null;
    private String event_type = null;
    private String uid = null;
    private int base_price = 0;

    public Events() {}

    // ⚙️ Constructor đầy đủ
    public Events(String event_name, String event_descrip, String event_start,
                 String event_end, String cast, String location, String event_type, String uid, int base_price) {
        this.event_name = event_name;
        this.event_descrip = event_descrip;
        this.event_start = event_start;
        this.event_end = event_end;
        this.cast = cast;
        this.location = location;
        this.event_type = event_type;
        this.uid = uid;
        this.base_price = base_price;
    }

    // 🧩 Getter & Setter
    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public String getEvent_descrip() {
        return event_descrip;
    }

    public void setEvent_descrip(String event_descrip) {
        this.event_descrip = event_descrip;
    }

    public String getEvent_start() {
        return event_start;
    }

    public void setEvent_start(String event_start) {
        this.event_start = event_start;
    }

    public String getEvent_end() {
        return event_end;
    }

    public void setEvent_end(String event_end) {
        this.event_end = event_end;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public void setUid(String uid){
        this.uid = uid;
    }

    public int getBase_price() {
        return base_price;
    }

    public void setBase_price(int base_price) {
        this.base_price = base_price;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("event_name", event_name);
        map.put("event_descrip", event_descrip);
        map.put("event_start", event_start);
        map.put("event_end", event_end);
        map.put("cast", cast);
        // Lưu cả key cũ và mới để không làm hỏng data đọc ở nơi khác
        map.put("location", location);
        map.put("event_location", location);
        map.put("event_type", event_type);
        map.put("organizer_uid", uid);
        map.put("base_price", base_price);
        map.put("min_price", base_price);
        return map;
    }

    @NonNull
    @Override
    public String toString() {
        return "Events{" +
                "event_name='" + event_name + '\'' + ",\n"
                + "event_descrip='" + event_descrip + '\'' + ",\n"
                + "event_start='" + event_start + '\'' + ",\n"
                + "event_end='" + event_end + '\'' + ",\n"
                + "cast='" + cast + '\'' + ",\n"
                + "location='" + location + '\'' + ",\n"
                + "event_type='" + event_type + '\'' + ",\n"
                + "base_price='" + base_price + '\'' + ",\n"
                + "uid='" + uid + '\'';


    }

    public static ArrayList<String> getEventType(){
        ArrayList<String> arrEvent = new ArrayList<>();
        arrEvent.add("Concert");
        arrEvent.add("Music");
        arrEvent.add("Sport");
        arrEvent.add("Innovation");
        arrEvent.add("Other");
        return arrEvent;
    }
}
