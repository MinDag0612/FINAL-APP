package com.FinalProject.feature_profile.data;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_profile.model.ProfileInfo;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileRepository {

    public interface ProfileCallback {
        void onSuccess(ProfileInfo info);
        void onError(String message);
    }

    public interface ProfileUpdateCallback {
        void onSuccess(ProfileInfo info);
        void onError(String message);
    }

    private final ProfileApi api;

    public ProfileRepository() {
        this(new FirebaseProfileApi());
    }

    public ProfileRepository(ProfileApi api) {
        this.api = api;
    }

    public void getProfile(ProfileCallback callback) {
        FirebaseUser currentUser = api.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        api.getUserInfor(currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        callback.onSuccess(mapProfile(snapshot));
                    } else {
                        callback.onError("Không tìm thấy thông tin người dùng.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateProfile(@NonNull ProfileInfo info, ProfileUpdateCallback callback) {
        FirebaseUser currentUser = api.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        String fullname = normalize(info.getFullName());
        String phone = normalize(info.getPhone());

        if (fullname != null) {
            updates.put(StoreField.UserFields.FULLNAME, fullname);
        }
        if (phone != null) {
            updates.put(StoreField.UserFields.PHONE, phone);
        }

        if (updates.isEmpty()) {
            callback.onError("Không có thay đổi để lưu.");
            return;
        }

        api.updateUserInfor(currentUser.getUid(), updates)
                .addOnSuccessListener(ignored -> refreshProfile(currentUser.getUid(), callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void refreshProfile(String uid, ProfileUpdateCallback callback) {
        api.getUserInfor(uid)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        callback.onSuccess(mapProfile(snapshot));
                    } else {
                        callback.onError("Không thể tải lại thông tin.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private ProfileInfo mapProfile(DocumentSnapshot snapshot) {
        String fullName = snapshot.getString(StoreField.UserFields.FULLNAME);
        String email = snapshot.getString(StoreField.UserFields.EMAIL);
        String phone = snapshot.getString(StoreField.UserFields.PHONE);
        String role = snapshot.getString(StoreField.UserFields.ROLE);
        return new ProfileInfo(
                fullName != null ? fullName : "",
                email != null ? email : "",
                phone != null ? phone : "",
                role != null ? role : ""
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    public void logout() {
        api.logout();
    }
}
