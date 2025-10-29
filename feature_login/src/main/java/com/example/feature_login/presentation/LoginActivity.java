package com.example.feature_login.presentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feature_login.R;
import com.example.feature_login.data.remote.FakeUserApi;
import com.example.feature_login.data.repository.AccountRepositoryImpl;
import com.example.feature_login.domain.usecase.LoginUseCase;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    Button btn_role_organizer;
    Button btn_role_attendee;
    TextInputLayout email;
    TextInputLayout password;
    Button btn_submit_login;
    Button btn_login_with_google;
    MaterialButtonToggleGroup btn_group;
    String selectedRole = "";
    String emailStr;
    String passwordStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initViews();
        setBtn_role();
        setBtn_submit_login();
    }

    private void initViews() {
        btn_group = findViewById(R.id.group_role_toggle);
        btn_role_organizer = findViewById(R.id.btn_role_organizer);
        btn_role_attendee = findViewById(R.id.btn_role_attendee);
        email = findViewById(R.id.input_email);
        password = findViewById(R.id.input_password);
        btn_submit_login = findViewById(R.id.btn_submit_login);
        btn_login_with_google = findViewById(R.id.btn_login_with_google);
    }

    private void setBtn_role() {
        btn_group.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_role_organizer) {
                    selectedRole = "organizer";
                }
                if (checkedId == R.id.btn_role_attendee) {
                    selectedRole = "attendee";
                }
            }
        });
    }

    private void setBtn_submit_login() {
        btn_submit_login.setOnClickListener(view -> {
            emailStr = email.getEditText().getText().toString().trim();
            passwordStr = password.getEditText().getText().toString().trim();

            if (emailStr.isEmpty() || passwordStr.isEmpty() || selectedRole == "") {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                LoginUseCase loginUseCase = new LoginUseCase(new AccountRepositoryImpl(new FakeUserApi()));
                boolean status = loginUseCase.excute(emailStr, passwordStr, selectedRole);
                if (status) {
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(this, "Login status False", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
