package com.FinalProject.core.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Map;

public class FirebaseAuthHelper {
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();


    public static Task<AuthResult> login(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);

    }

    public static void logout() {
        mAuth.signOut();
    }

    // REGISTER EMAIL UNIQUE AND PASSWORD MUST BE STRONG HERE

    public static Task<AuthResult> register(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    public static String getCurrentUserUid() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }


}
