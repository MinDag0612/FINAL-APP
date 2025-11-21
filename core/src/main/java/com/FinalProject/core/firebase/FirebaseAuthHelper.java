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
import java.util.Objects;

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

    public static Task<Void> resetPassword(String email) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        if (email == null || email.isEmpty()) {
            tcs.setException(new IllegalArgumentException("Email trống"));
            return tcs.getTask();
        }
        return mAuth.sendPasswordResetEmail(email);
    }

    public static Task<Boolean> checkEmailExists(String email) {
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        if (email == null || email.isEmpty()) {
            tcs.setResult(false);
            return tcs.getTask();
        }
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    boolean exists = result != null && result.getSignInMethods() != null
                            && !Objects.requireNonNull(result.getSignInMethods()).isEmpty();
                    tcs.setResult(exists);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }


}
