package com.FinalProject.feature_login.data;

import com.FinalProject.core.firebase.FirebaseAuthHelper;
import com.FinalProject.core.util.UserInfor_API;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginRepositoryImpl {

    public Task<AuthResult> login(String email, String password, String role) {
        return FirebaseAuthHelper.login(email, password);
    }

    public Task<DocumentSnapshot> getUserInforByEmail(String email) {
        return UserInfor_API.getUserInforByEmail(email);
    }
}
