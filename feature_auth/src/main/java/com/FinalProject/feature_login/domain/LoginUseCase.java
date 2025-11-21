package com.FinalProject.feature_login.domain;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.feature_login.data.LoginRepositoryImpl;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

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
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()){
            callback.onFailure("Email or password is empty");
            return;
        }
        Task<AuthResult> task = repo.login(email, password);
        task.addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                String uid = task1.getResult().getUser().getUid();

                repo.getUserInforById(uid)
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.exists()) {
                                callback.onFailure("User not found");
                            } else {
                                String roleDb = queryDocumentSnapshots.getString(StoreField.UserFields.ROLE);
                                if (roleDb == null || roleDb.trim().isEmpty()) {
                                    callback.onFailure("User role missing in database");
                                    return;
                                }
                                callback.onSuccess(uid, roleDb);
                            }
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } else {
                callback.onFailure(task1.getException().getMessage());
            }
        });
    }
}
