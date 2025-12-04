package com.FinalProject.core.util;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Artist_API - Quản lý CRUD operations cho Artists collection
 * 
 * Collections:
 * - Artists: Danh sách tất cả nghệ sĩ
 * - users/{uid}/following_artists: Nghệ sĩ mà user theo dõi
 */
public class Artist_API {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ARTISTS_COLLECTION = "Artists";
    private static final String FOLLOWING_SUBCOLLECTION = "following_artists";

    // ===== ARTISTS COLLECTION =====

    /**
     * Lấy tất cả nghệ sĩ
     */
    public static Task<QuerySnapshot> getAllArtists() {
        return db.collection(ARTISTS_COLLECTION)
                .orderBy("follower_count", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy nghệ sĩ theo ID
     */
    public static Task<DocumentSnapshot> getArtistById(@NonNull String artistId) {
        return db.collection(ARTISTS_COLLECTION)
                .document(artistId)
                .get();
    }

    /**
     * Tìm kiếm nghệ sĩ theo tên
     */
    public static Task<QuerySnapshot> searchArtistsByName(@NonNull String keyword) {
        String searchEnd = keyword + "\uf8ff";
        return db.collection(ARTISTS_COLLECTION)
                .whereGreaterThanOrEqualTo("artist_name", keyword)
                .whereLessThan("artist_name", searchEnd)
                .limit(20)
                .get();
    }

    /**
     * Lấy nghệ sĩ theo thể loại
     */
    public static Task<QuerySnapshot> getArtistsByGenre(@NonNull String genre) {
        return db.collection(ARTISTS_COLLECTION)
                .whereEqualTo("artist_genre", genre)
                .limit(50)
                .get();
    }

    /**
     * Thêm nghệ sĩ mới (Admin/Organizer only)
     */
    public static Task<DocumentReference> addArtist(@NonNull Map<String, Object> artistData) {
        return db.collection(ARTISTS_COLLECTION).add(artistData);
    }

    /**
     * Cập nhật follower count
     */
    public static Task<Void> incrementFollowerCount(@NonNull String artistId) {
        return db.collection(ARTISTS_COLLECTION)
                .document(artistId)
                .update("follower_count", FieldValue.increment(1));
    }

    public static Task<Void> decrementFollowerCount(@NonNull String artistId) {
        return db.collection(ARTISTS_COLLECTION)
                .document(artistId)
                .update("follower_count", FieldValue.increment(-1));
    }

    // ===== USER FOLLOWING SUBCOLLECTION =====

    /**
     * User follow một nghệ sĩ
     * Lưu vào users/{uid}/following_artists/{artistId}
     */
    public static Task<Void> followArtist(@NonNull String userId, @NonNull String artistId) {
        Map<String, Object> data = new HashMap<>();
        data.put("artist_id", artistId);
        data.put("followed_at", System.currentTimeMillis());

        return db.collection(StoreField.USERS)
                .document(userId)
                .collection(FOLLOWING_SUBCOLLECTION)
                .document(artistId)
                .set(data)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return incrementFollowerCount(artistId);
                    }
                    throw task.getException();
                });
    }

    /**
     * User unfollow một nghệ sĩ
     */
    public static Task<Void> unfollowArtist(@NonNull String userId, @NonNull String artistId) {
        return db.collection(StoreField.USERS)
                .document(userId)
                .collection(FOLLOWING_SUBCOLLECTION)
                .document(artistId)
                .delete()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return decrementFollowerCount(artistId);
                    }
                    throw task.getException();
                });
    }

    /**
     * Kiểm tra user có đang follow nghệ sĩ này không
     */
    public static Task<Boolean> isFollowing(@NonNull String userId, @NonNull String artistId) {
        return db.collection(StoreField.USERS)
                .document(userId)
                .collection(FOLLOWING_SUBCOLLECTION)
                .document(artistId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().exists();
                    }
                    return false;
                });
    }

    /**
     * Lấy danh sách nghệ sĩ user đang follow
     */
    public static Task<QuerySnapshot> getFollowingArtists(@NonNull String userId) {
        return db.collection(StoreField.USERS)
                .document(userId)
                .collection(FOLLOWING_SUBCOLLECTION)
                .orderBy("followed_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy số lượng nghệ sĩ user đang follow
     */
    public static Task<Integer> getFollowingCount(@NonNull String userId) {
        return getFollowingArtists(userId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().size();
                    }
                    return 0;
                });
    }
}
