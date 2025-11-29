package com.FinalProject.feature_login.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_home.presentation.HomeActivity;
import com.FinalProject.feature_home_organizer.presentation.HomeOrganizerActivity;
import com.FinalProject.feature_login.R;
import com.FinalProject.feature_login.data.LoginRepositoryImpl;
import com.FinalProject.feature_login.domain.LoginUseCase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;


public class LoginActivity extends AppCompatActivity {
    TextInputLayout email;
    TextInputLayout password;
    Button btn_submit_login;
    Button btn_login_with_google;
    String emailStr;
    String passwordStr;
    TextView tv_to_signup;
    TextView tv_forgot_password;
    LoginUseCase loginUseCase;
    LoginRepositoryImpl logionRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        logionRepo = new LoginRepositoryImpl();
        loginUseCase = new LoginUseCase(logionRepo);

        initViews();
        setBtn_submit_login();
        setTv_to_signup();
        getDataFromSignUp();
    }



    private void initViews() {
        email = findViewById(R.id.input_email);
        password = findViewById(R.id.input_password);
        btn_submit_login = findViewById(R.id.btn_submit_login);
        btn_login_with_google = findViewById(R.id.btn_login_with_google);
        tv_to_signup = findViewById(R.id.tv_go_to_sign_up);
        tv_forgot_password = findViewById(R.id.tv_forgot_password);
    }

    private void getDataFromSignUp(){
        Intent intent = getIntent();
        if(intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            this.email.getEditText().setText(email);
            this.password.getEditText().setText(password);
        }
    }

    private void setTv_to_signup(){
        tv_to_signup.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        if (tv_forgot_password != null) {
            tv_forgot_password.setOnClickListener(v ->
                    startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }
    }

    private void setBtn_submit_login() {
        btn_submit_login.setOnClickListener(v -> {
            emailStr = email.getEditText() != null
                    ? email.getEditText().getText().toString().trim()
                    : "";
            passwordStr = password.getEditText() != null
                    ? password.getEditText().getText().toString()
                    : "";

            if (TextUtils.isEmpty(emailStr) || TextUtils.isEmpty(passwordStr)) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUseCase.execute(emailStr, passwordStr, new LoginUseCase.LoginCallback() {
                @Override
                public void onSuccess(String uid, String role) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                            .edit()
                            .putString("UID", uid)
                            .putString("ROLE", role)
                            .apply();
                    saveFcmToken(uid);

                    if ("organizer".equalsIgnoreCase(role)) {
                        startActivity(new Intent(LoginActivity.this, HomeOrganizerActivity.class));
                        finish();
                    } else {
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    }
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showResetPasswordDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View dialogView = inflater.inflate(R.layout.dialog_reset_email, null);
        TextInputLayout tilEmail = dialogView.findViewById(R.id.reset_email_input_layout);
        TextInputEditText input = dialogView.findViewById(R.id.reset_email_edit);

        if (input != null && email != null && email.getEditText() != null) {
            input.setText(email.getEditText().getText());
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu")
                .setMessage("Nhập email để nhận link đặt lại mật khẩu.")
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String emailStr = input != null ? input.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(emailStr)) {
                        Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    logionRepo.sendResetPassword(emailStr)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Đã gửi link đặt lại mật khẩu tới email.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openEventDetailScreen() {
        Intent detailIntent = new Intent("com.FinalProject.EVENT_DETAIL");
        detailIntent.putExtra("extra_event_title", "TEDxYouth Saigon 2024");
        detailIntent.putExtra("extra_event_schedule", "15.12.2024 • 18:30 - 22:00");
        detailIntent.putExtra("extra_event_location", "Nhà hát Thành phố, Quận 1");
        detailIntent.putExtra("extra_event_description",
                "TEDxYouth Saigon 2024 mang đến 8 câu chuyện truyền cảm hứng từ những người trẻ tiên phong trong giáo dục, nghệ thuật và công nghệ.");
        detailIntent.putExtra("extra_general_price", "690.000đ");
        detailIntent.putExtra("extra_vip_price", "1.290.000đ");
        detailIntent.putExtra("extra_vip_benefits",
                "• Ghế ngồi hàng đầu\n• Phòng chờ riêng và đồ uống\n• Bộ quà tặng và gặp gỡ diễn giả");

        ArrayList<String> tags = new ArrayList<>();
        tags.add("Innovation");
        tags.add("Youth");
        tags.add("Offline");
        detailIntent.putStringArrayListExtra("extra_tags", tags);

        ArrayList<String> timeline = new ArrayList<>();
        timeline.add("18:00 | Check-in & Welcome drink");
        timeline.add("19:00 | Phiên TEDx Session 1");
        timeline.add("21:00 | Networking VIP");
        detailIntent.putStringArrayListExtra("extra_timeline", timeline);

        startActivity(detailIntent);
    }

    private void saveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseFirestore.getInstance()
                            .collection("User_Infor")
                            .document(uid)
                            .update("fcm_token", token);
                });
    }
}
