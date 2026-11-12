package com.bingo.smsapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("sms_relay", MODE_PRIVATE);
        if (!prefs.getBoolean("registered", false)) {
            startActivity(new Intent(this, RegistrationActivity.class));
        }

        TextView textView = new TextView(this);
        textView.setText("SMS relay app is running\nWaiting for bank SMS...");
        textView.setPadding(32, 32, 32, 32);
        setContentView(textView);
    }
}
