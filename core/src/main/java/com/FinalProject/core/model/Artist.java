package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model đại diện cho Nghệ sĩ/Performer
 * Có thể là ca sĩ, ban nhạc, diễn giả, etc.
 */
public class Artist {
    private String artist_id;
    private String artist_name;
    private String artist_bio;          // Tiểu sử nghệ sĩ
    private String artist_image_url;    // Ảnh đại diện
    private String artist_genre;        // Thể loại: "pop", "rock", "edm", "hiphop"
    private int follower_count;         // Số người theo dõi
    private List<String> social_links;  // Facebook, Instagram, YouTube links
    private boolean is_verified;        // Nghệ sĩ được xác thực

    public Artist() {} // Firestore requires

    public Artist(String artist_id, String artist_name, String artist_bio, 
                  String artist_image_url, String artist_genre, int follower_count,
                  List<String> social_links, boolean is_verified) {
        this.artist_id = artist_id;
        this.artist_name = artist_name;
        this.artist_bio = artist_bio;
        this.artist_image_url = artist_image_url;
        this.artist_genre = artist_genre;
        this.follower_count = follower_count;
        this.social_links = social_links;
        this.is_verified = is_verified;
    }

    // Getters and Setters
    public String getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(String artist_id) {
        this.artist_id = artist_id;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public void setArtist_name(String artist_name) {
        this.artist_name = artist_name;
    }

    public String getArtist_bio() {
        return artist_bio;
    }

    public void setArtist_bio(String artist_bio) {
        this.artist_bio = artist_bio;
    }

    public String getArtist_image_url() {
        return artist_image_url;
    }

    public void setArtist_image_url(String artist_image_url) {
        this.artist_image_url = artist_image_url;
    }

    public String getArtist_genre() {
        return artist_genre;
    }

    public void setArtist_genre(String artist_genre) {
        this.artist_genre = artist_genre;
    }

    public int getFollower_count() {
        return follower_count;
    }

    public void setFollower_count(int follower_count) {
        this.follower_count = follower_count;
    }

    public List<String> getSocial_links() {
        return social_links;
    }

    public void setSocial_links(List<String> social_links) {
        this.social_links = social_links;
    }

    public boolean is_verified() {
        return is_verified;
    }

    public void setIs_verified(boolean is_verified) {
        this.is_verified = is_verified;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("artist_id", artist_id);
        map.put("artist_name", artist_name);
        map.put("artist_bio", artist_bio);
        map.put("artist_image_url", artist_image_url);
        map.put("artist_genre", artist_genre);
        map.put("follower_count", follower_count);
        map.put("social_links", social_links);
        map.put("is_verified", is_verified);
        return map;
    }
}
