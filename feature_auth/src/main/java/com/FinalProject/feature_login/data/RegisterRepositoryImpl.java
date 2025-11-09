package com.FinalProject.feature_login.data;

import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.core.util.UserInfor_API;
import com.google.firebase.auth.AuthResult;

import com.google.android.gms.tasks.Task;

import java.util.Map;

public class RegisterRepositoryImpl {

    public Task<AuthResult> register(String email, String password) {
        return FirebaseAuthHelper.register(email, password);
    }

    public String getCurrentUserUid() {
        return FirebaseAuthHelper.getCurrentUserUid();
    }

    public Task<Void> saveUserInfor(String uid, Map<String, Object> userInfor){
        return UserInfor_API.saveUserToFirestore(uid, userInfor);
    }
}
