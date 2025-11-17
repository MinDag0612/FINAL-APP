package com.FinalProject.feature_profile.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.FinalProject.feature_profile.R;
import com.FinalProject.feature_profile.data.ProfileRepository;
import com.FinalProject.feature_profile.domain.GetProfileInfoUseCase;
import com.FinalProject.feature_profile.domain.LogoutUseCase;
import com.FinalProject.feature_profile.domain.UpdateProfileInfoUseCase;
import com.FinalProject.feature_profile.model.ProfileInfo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearProgressIndicator progressIndicator;
    private TextView tvProfileName;
    private TextView tvProfileRole;
    private TextView tvProfileEmail;
    private TextView tvProfileInitial;
    private TextView tvProfileEmailValue;
    private TextView tvProfileRoleValue;
    private TextInputLayout inputFullName;
    private TextInputLayout inputPhone;
    private MaterialButton btnSave;
    private MaterialButton btnLogout;

    private GetProfileInfoUseCase getProfileInfoUseCase;
    private UpdateProfileInfoUseCase updateProfileInfoUseCase;
    private LogoutUseCase logoutUseCase;
    private ProfileInfo currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_FeatureProfile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initUseCases();
        initViews();
        setupToolbar();
        setupListeners();
        loadProfile();
    }

    private void initUseCases() {
        ProfileRepository repository = new ProfileRepository();
        getProfileInfoUseCase = new GetProfileInfoUseCase(repository);
        updateProfileInfoUseCase = new UpdateProfileInfoUseCase(repository);
        logoutUseCase = new LogoutUseCase(repository);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_profile);
        swipeRefreshLayout = findViewById(R.id.swipe_profile);
        progressIndicator = findViewById(R.id.progress_profile);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileRole = findViewById(R.id.tv_profile_role);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileInitial = findViewById(R.id.tv_profile_initial);
        tvProfileEmailValue = findViewById(R.id.tv_profile_email_value);
        tvProfileRoleValue = findViewById(R.id.tv_profile_role_value);
        inputFullName = findViewById(R.id.input_profile_fullname);
        inputPhone = findViewById(R.id.input_profile_phone);
        btnSave = findViewById(R.id.btn_profile_save);
        btnLogout = findViewById(R.id.btn_profile_logout);
        btnSave.setEnabled(false);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadProfile);
        btnSave.setOnClickListener(v -> attemptUpdate());
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadProfile() {
        showLoading(true);
        getProfileInfoUseCase.execute(new GetProfileInfoUseCase.Callback() {
            @Override
            public void onSuccess(ProfileInfo info) {
                showLoading(false);
                bindProfile(info);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showError(message);
            }
        });
    }

    private void bindProfile(ProfileInfo info) {
        currentProfile = info;
        tvProfileName.setText(defaultIfEmpty(info.getFullName(), getString(R.string.profile_empty_state)));
        tvProfileRole.setText(info.getRole());
        tvProfileEmail.setText(info.getEmail());
        tvProfileEmailValue.setText(info.getEmail());
        tvProfileRoleValue.setText(info.getRole());
        setInputText(inputFullName, info.getFullName());
        setInputText(inputPhone, info.getPhone());
        setInitial(info.getFullName());
        btnSave.setEnabled(true);
    }

    private void setInitial(String fullName) {
        String letter = "T";
        if (!TextUtils.isEmpty(fullName)) {
            letter = fullName.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        tvProfileInitial.setText(letter);
    }

    private void attemptUpdate() {
        clearInputErrors();
        String fullName = getText(inputFullName);
        String phone = getText(inputPhone);

        if (TextUtils.isEmpty(fullName)) {
            inputFullName.setError(getString(R.string.profile_required_field));
            return;
        }

        if (!TextUtils.isEmpty(phone) && phone.length() < 9) {
            inputPhone.setError(getString(R.string.profile_phone_error));
            return;
        }

        if (currentProfile != null
                && fullName.equals(currentProfile.getFullName())
                && phone.equals(currentProfile.getPhone())) {
            showMessage(getString(R.string.profile_no_changes));
            return;
        }

        showLoading(true);
        ProfileInfo updatedInfo = new ProfileInfo(
                fullName,
                currentProfile != null ? currentProfile.getEmail() : "",
                phone,
                currentProfile != null ? currentProfile.getRole() : ""
        );

        updateProfileInfoUseCase.execute(updatedInfo, new UpdateProfileInfoUseCase.Callback() {
            @Override
            public void onSuccess(ProfileInfo info) {
                showLoading(false);
                currentProfile = info;
                bindProfile(info);
                showMessage(getString(R.string.profile_update_success));
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showError(message);
            }
        });
    }

    private void performLogout() {
        logoutUseCase.execute();
        Toast.makeText(this, R.string.profile_logout_message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), "com.FinalProject.feature_login.presentation.LoginActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void showLoading(boolean isLoading) {
        swipeRefreshLayout.setRefreshing(isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading && currentProfile != null);
    }

    private void clearInputErrors() {
        inputFullName.setError(null);
        inputPhone.setError(null);
    }

    private void setInputText(TextInputLayout layout, String value) {
        TextInputEditText editText = (TextInputEditText) layout.getEditText();
        if (editText != null) {
            editText.setText(value);
        }
    }

    private String getText(TextInputLayout layout) {
        TextInputEditText editText = (TextInputEditText) layout.getEditText();
        return editText != null ? editText.getText().toString().trim() : "";
    }

    private void showError(String message) {
        String finalMessage = TextUtils.isEmpty(message)
                ? getString(R.string.profile_error_unknown)
                : message;
        Snackbar.make(swipeRefreshLayout, finalMessage, Snackbar.LENGTH_LONG).show();
    }

    private void showMessage(String message) {
        Snackbar.make(swipeRefreshLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    private String defaultIfEmpty(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
