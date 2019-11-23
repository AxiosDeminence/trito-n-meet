package com.example.tritonmeet;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {

    TextView mTv;
    Button mBtn;
    Calendar c;
    DatePickerDialog dpd;

    TextView mTv2;
    Button mBtn2;
    Calendar c2;
    DatePickerDialog dpd2;

    TextView mTimeTextView;
    Button mPickTimeButton;
    Calendar calendar1;
    Context mContext = this;

    TextView mTimeTextView2;
    Button mPickTimeButton2;
    Calendar calendar2;
    Context mContext2 = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        Spinner mySpinner = (Spinner) findViewById(R.id.spinner1);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(AddEventActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.names));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);

        Button button = (Button)findViewById(R.id.button);
        if(mySpinner.getSelectedItem().toString().equals("No")) {
            button.setVisibility(View.VISIBLE);
        }

        if(mySpinner.getSelectedItem().toString().equals("Daily")) {
            button.setVisibility(View.VISIBLE);
        }

        CheckBox monday = findViewById(R.id.monday);
        CheckBox tuesday = findViewById(R.id.tuesday);
        CheckBox wednesday = findViewById(R.id.wednesday);
        CheckBox thursday = findViewById(R.id.thursday);
        CheckBox friday = findViewById(R.id.friday);
        CheckBox saturday = findViewById(R.id.saturday);
        CheckBox sunday = findViewById(R.id.sunday);
        if(mySpinner.getSelectedItem().toString().equals("Weekly")) {
            button.setVisibility(View.VISIBLE);
            monday.setVisibility(View.VISIBLE);
            tuesday.setVisibility(View.VISIBLE);
            wednesday.setVisibility(View.VISIBLE);
            thursday.setVisibility(View.VISIBLE);
            friday.setVisibility(View.VISIBLE);
            saturday.setVisibility(View.VISIBLE);
            sunday.setVisibility(View.VISIBLE);
        }

        if(mySpinner.getSelectedItem().toString().equals("Weekly")) {
            monday.setVisibility(View.VISIBLE);
            tuesday.setVisibility(View.VISIBLE);
            wednesday.setVisibility(View.VISIBLE);
            thursday.setVisibility(View.VISIBLE);
            friday.setVisibility(View.VISIBLE);
            saturday.setVisibility(View.VISIBLE);
            sunday.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
        }

        mTv = findViewById(R.id.txtView);
        mBtn = findViewById(R.id.btnPick);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                c = Calendar.getInstance();
                int day = c.get(Calendar.DAY_OF_MONTH);
                int month = c.get(Calendar.MONTH);
                int year = c.get(Calendar.YEAR);

                dpd = new DatePickerDialog(AddEventActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int mYear, int mMonth, int mDay) {
                        mTv.setText((mMonth + 1) + "/" + mDay + "/" + mYear);
                    }
                }, day, month, year);
                dpd.show();
            }
        });

        mTv2 = findViewById(R.id.txtView2);
        mBtn2 = findViewById(R.id.btnPick2);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                c2 = Calendar.getInstance();
                int day = c2.get(Calendar.DAY_OF_MONTH);
                int month = c2.get(Calendar.MONTH);
                int year = c2.get(Calendar.YEAR);

                dpd2 = new DatePickerDialog(AddEventActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int mYear, int mMonth, int mDay) {
                        mTv.setText((mMonth + 1) + "/" + mDay + "/" + mYear);
                    }
                }, day, month, year);
                dpd2.show();
            }
        });

        mTimeTextView = (TextView) findViewById(R.id.time_text_view);
        calendar1 = Calendar.getInstance();
        final int hour = calendar1.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar1.get(Calendar.MINUTE);
        mPickTimeButton = (Button) findViewById(R.id.pick_time_button);
        mPickTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mTimeTextView.setText(hourOfDay + ":" + minute);
                    }
                }, hour, minute, android.text.format.DateFormat.is24HourFormat(mContext));
            }
        });

        mTimeTextView = (TextView) findViewById(R.id.time_text_view2);
        calendar2 = Calendar.getInstance();
        final int hour2 = calendar2.get(Calendar.HOUR_OF_DAY);
        final int minute2 = calendar2.get(Calendar.MINUTE);
        mPickTimeButton2 = (Button) findViewById(R.id.pick_time_button2);
        mPickTimeButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mTimeTextView.setText(hourOfDay + ":" + minute);
                    }
                }, hour, minute, android.text.format.DateFormat.is24HourFormat(mContext));
            }
        });
    }
}