package com.example.tritonmeet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class LoginActivity extends AppCompatActivity {

    String myURL;
    TextView registerAcc;
    TextView forgotPass;
    EditText emailAddress;
    EditText passWord;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        myURL = "https://triton-meet.herokuapp.com/loginUser";
        forgotPass = findViewById(R.id.forgotPass);
        registerAcc = findViewById(R.id.registerText);
        emailAddress = findViewById(R.id.loginEmail);
        passWord = findViewById(R.id.loginPass);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                login(view);
            }
        });

        registerAcc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        forgotPass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this, Forgot1Activity.class);
                startActivity(i);
            }
        });
    }

    public void login(View view) {
        String stringEmail = emailAddress.getText().toString().trim();
        String stringPass = passWord.getText().toString().trim();

        AsyncHttpClient client = new AsyncHttpClient();
        Context context = this.getApplicationContext();
        JSONObject user;
        StringEntity entity;
        try {
            user = new JSONObject();
            user.put("email", stringEmail);
            user.put("password", stringPass);
            entity = new StringEntity(user.toString(), "UTF-8");
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("unexpected error", e);
        }

        client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Context context = getApplicationContext();
                CharSequence text;
                int duration = Toast.LENGTH_SHORT;

                if (statusCode == 400) {
                    text = "User does not exist";
                }
                else if (statusCode == 401) {
                    text = "User and password do not match";
                }
                else {
                    text = "Error " + statusCode + ": " + error;
                }

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });

    }
}
