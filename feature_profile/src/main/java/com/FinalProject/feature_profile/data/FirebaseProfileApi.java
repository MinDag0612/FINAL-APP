package com.FinalProject.feature_profile.data;

import com.FinalProject.core.util.UserInfor_API;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

public class FirebaseProfileApi implements ProfileApi {

    private final FirebaseAuth auth;

    public FirebaseProfileApi() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseProfileApi(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @Override
    public Task<DocumentSnapshot> getUserInfor(String uid) {
        return UserInfor_API.getUserInforById(uid);
    }

    @Override
    public Task<Void> updateUserInfor(String uid, Map<String, Object> fields) {
        return UserInfor_API.updateUserInfor(uid, fields);
    }

    @Override
    public void logout() {
        auth.signOut();
    }
}
