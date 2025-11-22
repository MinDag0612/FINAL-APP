package com.FinalProject.feature_login.domain;

import com.FinalProject.core.model.UserInfor;
import com.FinalProject.feature_login.data.RegisterRepositoryImpl;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserUseCase {
    public interface RegisterCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private final RegisterRepositoryImpl repo;

    public RegisterUserUseCase(RegisterRepositoryImpl repo) {
        this.repo = repo;
    }

    public void execute(String email, String password, String role, String fullname, String phone ,RegisterCallback callback) {
        if (email.isEmpty() || password.isEmpty() || role.isEmpty() || role.equals("") || fullname.isEmpty() || phone.isEmpty()) {
            callback.onFailure("Please fill in all fields");
        } else {
            Task<AuthResult> result = repo.register(email, password);
            result.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = repo.getCurrentUserUid();

                    UserInfor user = new UserInfor(fullname, phone, email, role);

                    repo.saveUserInfor(uid, user.toMap())
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                }
                else {
                    callback.onFailure(task.getException().getMessage());
                }
            });
        }
    }
}
