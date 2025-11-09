package com.FinalProject.mainActivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_login.presentation.LoginActivity;
import com.FinalProject.core.util.Seeder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Seeder.runSeed();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}