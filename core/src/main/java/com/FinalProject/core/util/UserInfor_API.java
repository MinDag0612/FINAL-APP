package com.FinalProject.core.util;

import com.FinalProject.core.constName.StoreField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class UserInfor_API {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // QUERY = "INSERT USER UID, USER_INFOR INTO USERS"
    // USER_INFOR = {fullname, email, phone, role}
    public static Task<Void> saveUserToFirestore(String uid, Map<String, Object> userInfor) {
        return db.collection(StoreField.USER_INFOR)
                .document(uid)
                .set(userInfor);
    }

    // QUERY = "SELECT * FROM USERS WHERE email = "email"
    public static Task<DocumentSnapshot> getUserInforByEmail(String email){
        TaskCompletionSource<DocumentSnapshot> tcs = new TaskCompletionSource<>();
        Query query = db.collection(StoreField.USER_INFOR)
                .whereEqualTo(StoreField.UserFields.EMAIL, email)
                .limit(1);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        tcs.setResult(documentSnapshot);
                    } else {
                        tcs.setException(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    public static Task<DocumentSnapshot> getUserInforById(String userId) {
        return db.collection(StoreField.USER_INFOR)
                .document(userId)
                .get();
    }

    public static Task<Void> updateUserInfor(String uid, Map<String, Object> updates) {
        if (uid == null) {
            return Tasks.forException(new IllegalArgumentException("User id is null"));
        }
        if (updates == null || updates.isEmpty()) {
            return Tasks.forResult(null);
        }
        return db.collection(StoreField.USER_INFOR)
                .document(uid)
                .set(updates, SetOptions.merge());
    }

    public static Task<DocumentSnapshot> getFcmTokenByUserId(String userId) {
        return db.collection(StoreField.USER_INFOR)
                .document(userId)
                .get();
    }
}
