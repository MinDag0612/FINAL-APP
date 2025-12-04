package com.FinalProject.core.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NewsFeed_API - Quản lý tin tức và news feed
 */
public class NewsFeed_API {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String NEWS_FEED_COLLECTION = "NewsFeed";
    private static final String NEWS_LIKES_COLLECTION = "NewsLikes";
    private static final String NEWS_VIEWS_COLLECTION = "NewsViews";

    /**
     * Lấy featured news (tin nổi bật)
     */
    public static Task<QuerySnapshot> getFeaturedNews() {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereEqualTo("is_featured", true)
                .orderBy("priority", Query.Direction.DESCENDING)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(5)
                .get();
    }

    /**
     * Lấy latest news với phân trang
     */
    public static Task<QuerySnapshot> getLatestNews(int limit) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    /**
     * Lấy news theo type
     */
    public static Task<QuerySnapshot> getNewsByType(@NonNull String newsType, int limit) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereEqualTo("news_type", newsType)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    /**
     * Lấy trending news (view count cao)
     */
    public static Task<QuerySnapshot> getTrendingNews(int limit) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .orderBy("view_count", Query.Direction.DESCENDING)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    /**
     * Tìm kiếm news theo keyword
     */
    public static Task<QuerySnapshot> searchNews(@NonNull String keyword) {
        String searchEnd = keyword + "\uf8ff";
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereGreaterThanOrEqualTo("title", keyword)
                .whereLessThan("title", searchEnd)
                .orderBy("title")
                .limit(20)
                .get();
    }

    /**
     * Tìm news theo tag
     */
    public static Task<QuerySnapshot> getNewsByTag(@NonNull String tag, int limit) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereArrayContains("tags", tag)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    /**
     * Lấy news liên quan đến event
     */
    public static Task<QuerySnapshot> getNewsForEvent(@NonNull String eventId) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereArrayContains("related_event_ids", eventId)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy news liên quan đến artist
     */
    public static Task<QuerySnapshot> getNewsForArtist(@NonNull String artistId) {
        return db.collection(NEWS_FEED_COLLECTION)
                .whereEqualTo("is_published", true)
                .whereArrayContains("related_artist_ids", artistId)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy chi tiết một news
     */
    public static Task<DocumentSnapshot> getNewsById(@NonNull String newsId) {
        return db.collection(NEWS_FEED_COLLECTION)
                .document(newsId)
                .get();
    }

    /**
     * Tăng view count
     */
    public static Task<Void> incrementViewCount(@NonNull String newsId,
                                               @NonNull String userId) {
        // Check if user already viewed
        return db.collection(NEWS_VIEWS_COLLECTION)
                .whereEqualTo("news_id", newsId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // If not viewed yet, increment and record
                    if (task.getResult().isEmpty()) {
                        // Record view
                        Map<String, Object> viewData = new HashMap<>();
                        viewData.put("news_id", newsId);
                        viewData.put("user_id", userId);
                        viewData.put("viewed_at", System.currentTimeMillis());

                        db.collection(NEWS_VIEWS_COLLECTION).add(viewData);

                        // Increment count
                        return db.collection(NEWS_FEED_COLLECTION)
                                .document(newsId)
                                .update("view_count", FieldValue.increment(1));
                    }

                    return null;
                });
    }

    /**
     * Like/Unlike news
     */
    public static Task<Boolean> toggleLike(@NonNull String newsId,
                                          @NonNull String userId) {
        return db.collection(NEWS_LIKES_COLLECTION)
                .whereEqualTo("news_id", newsId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    boolean wasLiked = !task.getResult().isEmpty();

                    if (wasLiked) {
                        // Unlike: remove like and decrement
                        String likeId = task.getResult().getDocuments().get(0).getId();
                        db.collection(NEWS_LIKES_COLLECTION).document(likeId).delete();

                        return db.collection(NEWS_FEED_COLLECTION)
                                .document(newsId)
                                .update("like_count", FieldValue.increment(-1))
                                .continueWith(t -> false); // Now unliked
                    } else {
                        // Like: add like and increment
                        Map<String, Object> likeData = new HashMap<>();
                        likeData.put("news_id", newsId);
                        likeData.put("user_id", userId);
                        likeData.put("liked_at", System.currentTimeMillis());

                        db.collection(NEWS_LIKES_COLLECTION).add(likeData);

                        return db.collection(NEWS_FEED_COLLECTION)
                                .document(newsId)
                                .update("like_count", FieldValue.increment(1))
                                .continueWith(t -> true); // Now liked
                    }
                });
    }

    /**
     * Kiểm tra user đã like news chưa
     */
    public static Task<Boolean> isLiked(@NonNull String newsId,
                                       @NonNull String userId) {
        return db.collection(NEWS_LIKES_COLLECTION)
                .whereEqualTo("news_id", newsId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return !task.getResult().isEmpty();
                    }
                    return false;
                });
    }

    /**
     * Tạo news mới (Admin/Organizer only)
     */
    public static Task<String> createNews(@NonNull Map<String, Object> newsData) {
        return db.collection(NEWS_FEED_COLLECTION)
                .add(newsData)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().getId();
                    }
                    throw task.getException();
                });
    }

    /**
     * Update news
     */
    public static Task<Void> updateNews(@NonNull String newsId,
                                       @NonNull Map<String, Object> updates) {
        updates.put("updated_at", System.currentTimeMillis());
        return db.collection(NEWS_FEED_COLLECTION)
                .document(newsId)
                .update(updates);
    }

    /**
     * Delete news (soft delete - set is_published = false)
     */
    public static Task<Void> deleteNews(@NonNull String newsId) {
        return db.collection(NEWS_FEED_COLLECTION)
                .document(newsId)
                .update("is_published", false);
    }

    /**
     * Lấy personalized news feed dựa trên interests của user
     * (following artists, attended events, etc.)
     */
    public static Task<QuerySnapshot> getPersonalizedFeed(@NonNull String userId) {
        // Step 1: Get user's following artists
        return Artist_API.getFollowingArtists(userId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        // Fallback to latest news
                        return getLatestNews(20);
                    }

                    // Get artist IDs
                    QuerySnapshot followingSnapshot = task.getResult();
                    if (followingSnapshot.isEmpty()) {
                        return getLatestNews(20);
                    }

                    // For simplicity, return latest news
                    // In production, should filter by related_artist_ids
                    return getLatestNews(20);
                });
    }

    /**
     * Get recommended news based on tags from previously viewed news
     */
    public static Task<QuerySnapshot> getRecommendedNews(@NonNull String userId, int limit) {
        // Implementation: analyze user's viewed news tags and recommend similar
        // For now, return trending news
        return getTrendingNews(limit);
    }
}
