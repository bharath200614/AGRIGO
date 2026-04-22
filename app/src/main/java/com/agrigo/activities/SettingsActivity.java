package com.agrigo.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.agrigo.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView)findViewById(R.id.tvTitle)).setText("Settings");
    }
}
