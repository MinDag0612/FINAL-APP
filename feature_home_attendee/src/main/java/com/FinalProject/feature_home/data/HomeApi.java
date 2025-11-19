package com.FinalProject.feature_home.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public interface HomeApi {

    FirebaseUser getCurrentUser();

    Task<DocumentSnapshot> getUserInfo(String email);

    Task<QuerySnapshot> getLatestEvents(int limit);

    Task<QuerySnapshot> getTicketsForEvent(String eventId);

    Task<QuerySnapshot> getOrdersByUserId(String userId);
}
