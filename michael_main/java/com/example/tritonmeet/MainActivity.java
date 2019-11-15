package com.example.tritonmeet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addEventButton;
    Button tempButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tempButton = findViewById(R.id.tempButton1);
        addEventButton = findViewById(R.id.fabAddEvent);

        tempButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToGroupActivity(view);
            }
        });

        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEvent(view);
            }
        });
    }

    public void goToGroupActivity(View view) {

        Intent retrieveOwner = getIntent();
        String owner = retrieveOwner.getStringExtra("email");

        Intent i = new Intent(MainActivity.this, Group1Activity.class);
        i.putExtra("email", owner);

        startActivity(i);
    }

    public void addEvent(View view) {
        Intent i = new Intent(MainActivity.this, AddEventActivity.class);
        startActivity(i);
    }
}
