package com.example.tritonmeet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addGroupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addGroupButton = findViewById(R.id.floating_action_button);

        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addGroup(view);
            }
        });

    }

    public void addGroup(View view) {
        Intent i = new Intent(MainActivity.this, GroupActivity.class);
        startActivity(i);
    }
}
