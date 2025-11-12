package com.FinalProject.feature_login.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_login.R;
import com.FinalProject.feature_login.data.LoginRepositoryImpl;
import com.FinalProject.feature_login.domain.LoginUseCase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    MaterialButton btn_role_organizer;
    MaterialButton btn_role_attendee;
    TextInputLayout email;
    TextInputLayout password;
    Button btn_submit_login;
    Button btn_login_with_google;
    MaterialButtonToggleGroup btn_group;
    String selectedRole = "";
    String emailStr;
    String passwordStr;
    String role;
    TextView tv_to_signup;
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
        setBtn_role();
        setBtn_submit_login();
        setTv_to_signup();
        getDataFromSignUp();
    }



    private void initViews() {
        btn_group = findViewById(R.id.group_role_toggle);
        btn_role_organizer = findViewById(R.id.btn_role_organizer);
        btn_role_attendee = findViewById(R.id.btn_role_attendee);
        email = findViewById(R.id.input_email);
        password = findViewById(R.id.input_password);
        btn_submit_login = findViewById(R.id.btn_submit_login);
        btn_login_with_google = findViewById(R.id.btn_login_with_google);
        tv_to_signup = findViewById(R.id.tv_go_to_sign_up);
    }

    private void getDataFromSignUp(){
        Intent intent = getIntent();
        if(intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            this.email.getEditText().setText(email);
            this.password.getEditText().setText(password);

            String role = intent.getStringExtra("role");
            if (role == null){
                return;
            }
            if (role.equals("organizer")){
                btn_role_organizer.setChecked(true);
            }
            if (role.equals("attendee")){
                btn_role_attendee.setChecked(true);
            }
        }
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

    private void setTv_to_signup(){
        tv_to_signup.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void setBtn_submit_login() {
        btn_submit_login.setOnClickListener(v -> {
            emailStr = email.getEditText().getText().toString();
            passwordStr = password.getEditText().getText().toString();
            role = selectedRole;

            loginUseCase.execute(emailStr, passwordStr, role, new LoginUseCase.LoginCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    openEventDetailScreen();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
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
}
