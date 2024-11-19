package com.example.streetfighter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        Button startButton = findViewById(R.id.startButton);

        // Navigate to MainActivity when Start button is clicked
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EntryActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Close the EntryActivity
            }
        });
    }
}
