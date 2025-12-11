package com.FinalProject.feature_login.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_login.R;
import com.FinalProject.feature_login.data.RegisterRepositoryImpl;
import com.FinalProject.feature_login.domain.RegisterUserUseCase;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    TextInputEditText full_name;
    TextInputEditText email;
    TextInputEditText phone_number;
    TextInputEditText password;
    Button btn_submit_register;
    String selectedRole = "attendee"; // Mặc định là attendee
    String fullNameStr;
    String emailStr;
    String phoneNumberStr;
    String passwordStr;
    TextView btn_login;
    ImageButton btn_back;
    RegisterUserUseCase regisreUseCase;
    RegisterRepositoryImpl repo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        initViews();
        setBtn_submit_register();
        setBtn_login();
    }

    private void initViews() { // TRÙNG ID VỚI LOGIN
        full_name = findViewById(R.id.register_fullname);
        email = findViewById(R.id.register_email);
        phone_number = findViewById(R.id.register_phone);
        password = findViewById(R.id.register_password);
        btn_submit_register = findViewById(R.id.btn_submit_register);
        btn_login = findViewById(R.id.tv_go_to_login);
        btn_back = findViewById(R.id.btn_register_back);

        repo = new RegisterRepositoryImpl();
        regisreUseCase = new RegisterUserUseCase(repo);

        if (btn_back != null) {
            btn_back.setOnClickListener(v ->
                    getOnBackPressedDispatcher().onBackPressed());
        }
    }

//    private void setBtn_submit_register() {
//        btn_submit_register.setOnClickListener(view -> {
//            fullNameStr = full_name.getText().toString().trim();
//            emailStr = email.getText().toString().trim();
//            phoneNumberStr = phone_number.getText().toString().trim();
//            passwordStr = password.getText().toString().trim();
//
//            if (fullNameStr.isEmpty() || emailStr.isEmpty() || phoneNumberStr.isEmpty() || passwordStr.isEmpty() || selectedRole.isEmpty()) {
//                // Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
//            } else {
//                // TODO: Implement SignupUseCase logic here
//                // Toast.makeText(this, "Đăng ký thành công (placeholder)", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    private void setBtn_login() {
        btn_login.setOnClickListener(view -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }


    private void setBtn_submit_register(){
        btn_submit_register.setOnClickListener(view -> {
            emailStr = email.getText().toString().trim();
            passwordStr = password.getText().toString().trim();
            fullNameStr = full_name.getText().toString().trim();
            phoneNumberStr = phone_number.getText().toString().trim();

            regisreUseCase.execute(emailStr, passwordStr, selectedRole, fullNameStr, phoneNumberStr, new RegisterUserUseCase.RegisterCallback(){
                @Override
                public void onSuccess() {
                    // Toast.makeText(RegisterActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("email", emailStr);
                    intent.putExtra("password", passwordStr);
                    intent.putExtra("role", selectedRole);
                    startActivity(intent);
                }

                @Override
                public void onFailure(String message) {
                    // Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
