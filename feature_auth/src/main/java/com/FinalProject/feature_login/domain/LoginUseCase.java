package com.FinalProject.feature_login.domain;

import com.FinalProject.feature_login.data.LoginRepositoryImpl;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;

public class LoginUseCase {

    public interface LoginCallback {
        void onSuccess();
        void onFailure(String message);
    }
    private final LoginRepositoryImpl repo;

    public LoginUseCase(LoginRepositoryImpl repo) {
        this.repo = repo;
    }

    public void execute(String email, String password, String role, LoginCallback callback) {
        if (email.isEmpty() || password.isEmpty()){
            callback.onFailure("Email or password is empty");
            return;
        }
        Task<AuthResult> task = repo.login(email, password, role);
        task.addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                repo.getUserInforByEmail(email)
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.exists()) {
                                callback.onFailure("User not found");
                            } else {
                                String roleDb = queryDocumentSnapshots.getString("role");
                                if (!roleDb.equals(role)) {
                                    callback.onFailure("Role not match. Please check your role again");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } else {
                callback.onFailure(task1.getException().getMessage());
            }
        });
    }
}
