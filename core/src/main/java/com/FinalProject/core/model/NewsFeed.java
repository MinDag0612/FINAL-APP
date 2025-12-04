package com.FinalProject.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model cho News Feed
 * Hiển thị tin tức, sự kiện hot, trending artists
 */
public class NewsFeed {
    private String news_id;
    private String title;
    private String content;
    private String image_url;
    private String thumbnail_url;
    private String news_type;               // "event", "artist", "general", "promotion"
    private List<String> related_event_ids; // Events liên quan
    private List<String> related_artist_ids; // Artists liên quan
    private List<String> tags;              // ["concert", "music", "trending"]
    private long published_at;
    private long updated_at;
    private int view_count;
    private int like_count;
    private String author_id;               // User/Admin đăng tin
    private String author_name;
    private boolean is_featured;            // Tin nổi bật
    private boolean is_published;           // Đã xuất bản
    private int priority;                   // Độ ưu tiên hiển thị (1-10)

    public NewsFeed() {} // Firestore requires

    public NewsFeed(String news_id, String title, String content, String image_url,
                   String thumbnail_url, String news_type, List<String> related_event_ids,
                   List<String> related_artist_ids, List<String> tags, long published_at,
                   long updated_at, int view_count, int like_count, String author_id,
                   String author_name, boolean is_featured, boolean is_published, int priority) {
        this.news_id = news_id;
        this.title = title;
        this.content = content;
        this.image_url = image_url;
        this.thumbnail_url = thumbnail_url;
        this.news_type = news_type;
        this.related_event_ids = related_event_ids;
        this.related_artist_ids = related_artist_ids;
        this.tags = tags;
        this.published_at = published_at;
        this.updated_at = updated_at;
        this.view_count = view_count;
        this.like_count = like_count;
        this.author_id = author_id;
        this.author_name = author_name;
        this.is_featured = is_featured;
        this.is_published = is_published;
        this.priority = priority;
    }

    // Getters and Setters
    public String getNews_id() { return news_id; }
    public void setNews_id(String news_id) { this.news_id = news_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }

    public String getThumbnail_url() { return thumbnail_url; }
    public void setThumbnail_url(String thumbnail_url) { this.thumbnail_url = thumbnail_url; }

    public String getNews_type() { return news_type; }
    public void setNews_type(String news_type) { this.news_type = news_type; }

    public List<String> getRelated_event_ids() { return related_event_ids; }
    public void setRelated_event_ids(List<String> related_event_ids) { 
        this.related_event_ids = related_event_ids; 
    }

    public List<String> getRelated_artist_ids() { return related_artist_ids; }
    public void setRelated_artist_ids(List<String> related_artist_ids) { 
        this.related_artist_ids = related_artist_ids; 
    }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public long getPublished_at() { return published_at; }
    public void setPublished_at(long published_at) { this.published_at = published_at; }

    public long getUpdated_at() { return updated_at; }
    public void setUpdated_at(long updated_at) { this.updated_at = updated_at; }

    public int getView_count() { return view_count; }
    public void setView_count(int view_count) { this.view_count = view_count; }

    public int getLike_count() { return like_count; }
    public void setLike_count(int like_count) { this.like_count = like_count; }

    public String getAuthor_id() { return author_id; }
    public void setAuthor_id(String author_id) { this.author_id = author_id; }

    public String getAuthor_name() { return author_name; }
    public void setAuthor_name(String author_name) { this.author_name = author_name; }

    public boolean is_featured() { return is_featured; }
    public void setIs_featured(boolean is_featured) { this.is_featured = is_featured; }

    public boolean is_published() { return is_published; }
    public void setIs_published(boolean is_published) { this.is_published = is_published; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("news_id", news_id);
        map.put("title", title);
        map.put("content", content);
        map.put("image_url", image_url);
        map.put("thumbnail_url", thumbnail_url);
        map.put("news_type", news_type);
        map.put("related_event_ids", related_event_ids);
        map.put("related_artist_ids", related_artist_ids);
        map.put("tags", tags);
        map.put("published_at", published_at);
        map.put("updated_at", updated_at);
        map.put("view_count", view_count);
        map.put("like_count", like_count);
        map.put("author_id", author_id);
        map.put("author_name", author_name);
        map.put("is_featured", is_featured);
        map.put("is_published", is_published);
        map.put("priority", priority);
        return map;
    }

    /**
     * Helper method để format thời gian relative
     */
    public String getRelativeTime() {
        long now = System.currentTimeMillis();
        long diff = now - published_at;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " ngày trước";
        } else if (hours > 0) {
            return hours + " giờ trước";
        } else if (minutes > 0) {
            return minutes + " phút trước";
        } else {
            return "Vừa xong";
        }
    }
}
