package com.FinalProject.feature_login.presentation;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_login.R;
import com.FinalProject.feature_login.data.LoginRepositoryImpl;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Màn quên mật khẩu: kiểm tra email hợp lệ, gửi email reset, xử lý lỗi user not found.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSend;
    private ImageButton btnBack;

    private LoginRepositoryImpl repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        repo = new LoginRepositoryImpl();
        initViews();
        bindActions();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.forgot_email_input_layout);
        edtEmail = findViewById(R.id.forgot_email_edit);
        btnSend = findViewById(R.id.btn_send_reset);
        btnBack = findViewById(R.id.btn_forgot_back);
    }

    private void bindActions() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        btnSend.setOnClickListener(v -> {
            String email = edtEmail != null && edtEmail.getText() != null
                    ? edtEmail.getText().toString().trim()
                    : "";
            if (!isValidEmail(email)) {
                tilEmail.setError("Email không hợp lệ");
                return;
            }
            tilEmail.setError(null);
            btnSend.setEnabled(false);
            btnSend.setText("Đang gửi...");

            sendReset(email);
        });
    }

    private void sendReset(String email) {
        repo.sendResetPassword(email)
                .addOnSuccessListener(unused -> {
                    // Toast.makeText(this, "Đã gửi liên kết đặt lại mật khẩu.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Gửi liên kết");
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        if ("ERROR_USER_NOT_FOUND".equals(code)) {
                            // Toast.makeText(this, "Email chưa đăng ký tài khoản.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
