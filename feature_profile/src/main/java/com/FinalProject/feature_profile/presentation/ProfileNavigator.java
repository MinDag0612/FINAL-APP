package com.FinalProject.feature_profile.presentation;

import android.content.Context;
import android.content.Intent;

public final class ProfileNavigator {

    private ProfileNavigator() {}

    public static Intent createIntent(Context context) {
        return new Intent(context, ProfileActivity.class);
    }
}
