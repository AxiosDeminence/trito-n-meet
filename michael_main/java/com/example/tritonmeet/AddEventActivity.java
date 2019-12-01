package com.example.tritonmeet;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class AddEventActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener {

    private EditText nameOfEvent;
    private Button addEvent;
    private Context context;

    private TextView textStartDate;
    private Button buttonStartDate;
    private DatePickerDialog dpd;

    private TextView textEndDate;
    private Button buttonEndDate;
    private DatePickerDialog dpd2;

    private TextView textStartTime;
    private Button buttonStartTime;

    private TextView textEndTime;
    private Button buttonEndTime;

    private DatePickerDialog.OnDateSetListener startDateListener;
    private DatePickerDialog.OnDateSetListener endDateListener;

    private TimePickerDialog.OnTimeSetListener startTimeListener;
    private TimePickerDialog.OnTimeSetListener endTimeListener;

    Spinner mySpinner;

    CheckBox monday;
    CheckBox tuesday;
    CheckBox wednesday;
    CheckBox thursday;
    CheckBox friday;
    CheckBox saturday;
    CheckBox sunday;

    String date1;
    String date2;
    String time1;
    String time2;

    String myURL = "https://triton-meet.herokuapp.com/manageEvents";
    private Intent retrieveCurrentUser;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        context = AddEventActivity.this;
        nameOfEvent = findViewById(R.id.enterEventName);

        retrieveCurrentUser = getIntent();
        currentUser = retrieveCurrentUser.getStringExtra("email");

        monday = findViewById(R.id.monday);
        tuesday = findViewById(R.id.tuesday);
        wednesday = findViewById(R.id.wednesday);
        thursday = findViewById(R.id.thursday);
        friday = findViewById(R.id.friday);
        saturday = findViewById(R.id.saturday);
        sunday = findViewById(R.id.sunday);

        mySpinner = findViewById(R.id.spinner1);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(AddEventActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.names));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);
        mySpinner.setOnItemSelectedListener(this);

        textStartDate = findViewById(R.id.startDateText);
        buttonStartDate = findViewById(R.id.pickStartDate);
        textEndDate = findViewById(R.id.endDateText);
        buttonEndDate = findViewById(R.id.pickEndDate);

        textStartTime = findViewById(R.id.startTimeText);
        buttonStartTime = findViewById(R.id.pickStartTime);
        textEndTime = findViewById(R.id.endTimeText);
        buttonEndTime = findViewById(R.id.pickEndTime);

        Calendar c = Calendar.getInstance();
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int month = c.get(Calendar.MONTH);
        final int year = c.get(Calendar.YEAR);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);

        addEvent = findViewById(R.id.addEvent);

        buttonStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(context, startDateListener, year, month, day);
                dialog.show();
            }
        });

        startDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                date1 = (month + 1) + "/" + dayOfMonth + "/" + year;
                date2 = (month + 1) + "/" + dayOfMonth + "/" + year;
                textStartDate.setText(date1);
                textEndDate.setText(date2);
            }
        };

        buttonEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(context, endDateListener, year, month, day);
                dialog.show();
            }
        });

        endDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                date2 = (month + 1) + "/" + dayOfMonth + "/" + year;
                textEndDate.setText(date2);
            }
        };

        buttonStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(context, startTimeListener, hour, minute,
                        android.text.format.DateFormat.is24HourFormat(context));
                dialog.show();
            }
        });

        startTimeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String time = hourOfDay + ":" + minute;
                time1 = Event.time24to12(time);
                textStartTime.setText(time1);
            }
        };

        buttonEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(context, endTimeListener, hour, minute,
                        android.text.format.DateFormat.is24HourFormat(context));
                dialog.show();
            }
        });

        endTimeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String time = hourOfDay + ":" + minute;
                time2 = Event.time24to12(time);
                textEndTime.setText(time2);
            }
        };

        addEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEvent(v);
            }
        });

    }

    public void addEvent(View view) {
        String stringNameEvent = nameOfEvent.getText().toString();
        String repeating = mySpinner.getSelectedItem().toString();

        String stringStartDate = textStartDate.getText().toString();
        String stringEndDate = textEndDate.getText().toString();
        String stringStartTime = textStartTime.getText().toString();
        String stringEndTime = textEndTime.getText().toString();

        String weekly = "";

        if (repeating.equals("Daily")) {
            weekly = "Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday";
        }
        if (repeating.equals("Weekly")) {
            if (monday.isChecked()) {
                weekly += "Monday,";
            }
            if (tuesday.isChecked()) {
                weekly += "Tuesday,";
            }
            if (wednesday.isChecked()) {
                weekly += "Wednesday,";
            }
            if (thursday.isChecked()) {
                weekly += "Thursday,";
            }
            if (friday.isChecked()) {
                weekly += "Friday,";
            }
            if (saturday.isChecked()) {
                weekly += "Saturday,";
            }
            if (sunday.isChecked()) {
                weekly += "Sunday,";
            }
            if (weekly.length() > 0) {
                weekly = weekly.substring(0, weekly.length() - 1);
            }
        }

        AsyncHttpClient client = new AsyncHttpClient();
        JSONObject eventJSON;
        StringEntity entity;
        try {
            eventJSON = new JSONObject();
            eventJSON.put("action", "create");
            eventJSON.put("email", currentUser);
            eventJSON.put("eventName", stringNameEvent);
            eventJSON.put("startDate", stringStartDate);
            eventJSON.put("endDate", stringEndDate);
            eventJSON.put("startTime", Event.time12to24(stringStartTime));
            eventJSON.put("endTime", Event.time12to24(stringEndTime));
            eventJSON.put("daysOfWeek", weekly);
            entity = new StringEntity(eventJSON.toString(), "UTF-8");
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("unexpected error", e);
        }

        client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(context, "Event created", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(AddEventActivity.this, MainActivity.class);
                i.putExtra("email", currentUser);
                startActivity(i);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(context, "Invalid event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String text = parent.getItemAtPosition(position).toString();

        if (text.equals("Weekly")) {
            monday.setVisibility(View.VISIBLE);
            tuesday.setVisibility(View.VISIBLE);
            wednesday.setVisibility(View.VISIBLE);
            thursday.setVisibility(View.VISIBLE);
            friday.setVisibility(View.VISIBLE);
            saturday.setVisibility(View.VISIBLE);
            sunday.setVisibility(View.VISIBLE);
        }
        else {
            monday.setVisibility(View.INVISIBLE);
            tuesday.setVisibility(View.INVISIBLE);
            wednesday.setVisibility(View.INVISIBLE);
            thursday.setVisibility(View.INVISIBLE);
            friday.setVisibility(View.INVISIBLE);
            saturday.setVisibility(View.INVISIBLE);
            sunday.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}