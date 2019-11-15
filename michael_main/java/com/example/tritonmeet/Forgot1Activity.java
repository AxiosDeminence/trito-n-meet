package com.example.tritonmeet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class Forgot1Activity extends AppCompatActivity {

    Button checkEmail;
    TextView emailCheckText;
    String myURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot1);

        checkEmail = findViewById(R.id.forgotButton);
        emailCheckText = findViewById(R.id.emailChecker);
        myURL = "INSERT REAL URL LATER";

        checkEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCodeToEmail(view);
            }
        });
    }

    public void sendCodeToEmail(View view) {
        String stringEmail = checkEmail.getText().toString().trim();

        if (stringEmail.length() == 0 || !(stringEmail.length() > 9 && stringEmail.substring(stringEmail.length() - 8).equals("ucsd.edu"))) {
            emailCheckText.setVisibility(View.VISIBLE);
            emailCheckText.setText("Invalid email");
        }
        else {
            emailCheckText.setVisibility(View.INVISIBLE);

            AsyncHttpClient client = new AsyncHttpClient();
            Context context = this.getApplicationContext();
            JSONObject emailJSON;

            StringEntity entity;
            try {
                emailJSON = new JSONObject();
                emailJSON.put("email", stringEmail);
                entity = new StringEntity(emailJSON.toString(), "UTF-8");
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("unexpected error", e);
            }

            client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Context context = getApplicationContext();
                    CharSequence text = "Success!";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    Intent i = new Intent(Forgot1Activity.this, Forgot2Activity.class);
                    startActivity(i);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Context context = getApplicationContext();
                    CharSequence text;

                    if (statusCode == 406) {
                        text = "Email not found";
                    }
                    else {
                        text = "Error " + statusCode + ": " + error;
                    }

                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            });
        }
    }
}
