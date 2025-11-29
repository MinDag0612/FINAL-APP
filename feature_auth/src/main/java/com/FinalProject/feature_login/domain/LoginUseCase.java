package com.FinalProject.feature_login.domain;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_login.data.LoginRepositoryImpl;
public class LoginUseCase {

    public interface LoginCallback {
        void onSuccess(String uid, String role);
        void onFailure(String message);
    }

    private final LoginRepositoryImpl repo;

    public LoginUseCase(LoginRepositoryImpl repo) {
        this.repo = repo;
    }

    public void execute(String email, String password, LoginCallback callback) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            callback.onFailure("Email or password is empty");
            return;
        }

        repo.login(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        repo.getUserInforById(uid)
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                                        callback.onFailure("User not found");
                                        return;
                                    }
                                    String role = documentSnapshot.getString(StoreField.UserFields.ROLE);
                                    if (role == null || role.trim().isEmpty()) {
                                        callback.onFailure("User role missing in database");
                                        return;
                                    }
                                    callback.onSuccess(uid, role);
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        callback.onFailure(message);
                    }
                });
    }
}
