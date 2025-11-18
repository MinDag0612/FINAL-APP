package com.FinalProject.feature_profile.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

public interface ProfileApi {

    FirebaseUser getCurrentUser();

    Task<DocumentSnapshot> getUserInfor(String uid);

    Task<Void> updateUserInfor(String uid, Map<String, Object> fields);

    void logout();
}
