package com.FinalProject.feature_login.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
    ChipGroup chip_group_role;
    Chip chip_role_attendee;
    Chip chip_role_organizer;
    TextInputEditText full_name;
    TextInputEditText email;
    TextInputEditText phone_number;
    TextInputEditText password;
    Button btn_submit_register;
    String selectedRole = "";
    String fullNameStr;
    String emailStr;
    String phoneNumberStr;
    String passwordStr;
    TextView btn_login;
    RegisterUserUseCase regisreUseCase;
    RegisterRepositoryImpl repo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        initViews();
        setChip_group_role();
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

        repo = new RegisterRepositoryImpl();
        regisreUseCase = new RegisterUserUseCase(repo);
        chip_group_role = findViewById(R.id.chip_group_role);
    }

    private void setChip_group_role() {
        chip_group_role.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_role_attendee)) {
                selectedRole = "attendee";
            } else if (checkedIds.contains(R.id.chip_role_organizer)) {
                selectedRole = "organizer";
            }
        });
    }

//    private void setBtn_submit_register() {
//        btn_submit_register.setOnClickListener(view -> {
//            fullNameStr = full_name.getText().toString().trim();
//            emailStr = email.getText().toString().trim();
//            phoneNumberStr = phone_number.getText().toString().trim();
//            passwordStr = password.getText().toString().trim();
//
//            if (fullNameStr.isEmpty() || emailStr.isEmpty() || phoneNumberStr.isEmpty() || passwordStr.isEmpty() || selectedRole.isEmpty()) {
//                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
//            } else {
//                // TODO: Implement SignupUseCase logic here
//                Toast.makeText(this, "Đăng ký thành công (placeholder)", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(RegisterActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("email", emailStr);
                    intent.putExtra("password", passwordStr);
                    intent.putExtra("role", selectedRole);
                    startActivity(intent);
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
