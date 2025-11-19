package com.FinalProject.feature_home.data;

import com.FinalProject.core.util.Event_API;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.core.util.TicketS_Infor_API;
import com.FinalProject.core.util.UserInfor_API;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseHomeApi implements HomeApi {

    private final FirebaseAuth auth;

    public FirebaseHomeApi() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseHomeApi(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @Override
    public Task<DocumentSnapshot> getUserInfo(String email) {
        return UserInfor_API.getUserInforByEmail(email);
    }

    @Override
    public Task<QuerySnapshot> getLatestEvents(int limit) {
        return Event_API.getEventASC(limit);
    }

    @Override
    public Task<QuerySnapshot> getTicketsForEvent(String eventId) {
        return TicketS_Infor_API.getTicketInforByEventId(eventId);
    }

    @Override
    public Task<QuerySnapshot> getOrdersByUserId(String userId) {
        return Order_API.getOrdersByUserId(userId);
    }
}
