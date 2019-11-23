package com.example.tritonmeet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

public class RegisterActivity extends AppCompatActivity {

    String myURL;
    Button registerButton;
    EditText fullName;
    EditText userEmail;
    EditText passWord1;
    EditText passWord2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        myURL = "https://triton-meet.herokuapp.com/createUser";
        registerButton = findViewById(R.id.registerButton);
        fullName = findViewById(R.id.fullName);
        userEmail = findViewById(R.id.emailAddress);
        passWord1 = findViewById(R.id.passWord);
        passWord2 = findViewById(R.id.cpassWord);

        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                register(view);
            }
        });
    }

    /**
     * Registers new user account into database
     * @param view View (unused)
     */
    public void register(View view) {
        String stringFullName = fullName.getText().toString().trim();
        String stringEmail = userEmail.getText().toString().trim();
        String stringPass1 = passWord1.getText().toString();
        String stringPass2 = passWord2.getText().toString();
        boolean validFullName;
        boolean validEmail;
        boolean validPassword;

        TextView checkOne = findViewById(R.id.check1);
        TextView checkTwo = findViewById(R.id.check2);
        TextView checkThree = findViewById(R.id.check3);

        final String PASSWORDCHECK = "((?=.*[a-z])(?=.*\\d)(?=.*[A-Z])(?=.*[!@#$%^&*()]).{6,30})";

        /**
         * Determine if user types anything for full name
         */
        if (stringFullName.length() == 0) {
            checkOne.setVisibility(View.VISIBLE);
            checkOne.setText("Invalid name");
            checkOne.setTextColor(Color.RED);
            validFullName = false;
        }
        else {
            checkOne.setVisibility(View.INVISIBLE);
            validFullName = true;
        }

        /**
         * Determines if user types valid UCSD email
         */
        if (stringEmail.length() == 0) {
            checkTwo.setVisibility(View.VISIBLE);
            checkTwo.setText("Invalid email");
            checkTwo.setTextColor(Color.RED);
            validEmail = false;
        }
        else if (!(stringEmail.length() > 9 && stringEmail.substring(stringEmail.length() - 8).equals("ucsd.edu"))) {
            checkTwo.setVisibility(View.VISIBLE);
            checkTwo.setText("Must be UCSD email");
            checkTwo.setTextColor(Color.RED);
            validEmail = false;
        }
        else {
            checkTwo.setVisibility(View.INVISIBLE);
            validEmail = true;
        }

        /**
         * Determines if two passwords are same and meets conditions
         */

        if (!(stringPass1.equals(stringPass2))) {
            checkThree.setText("Different passwords");
            checkThree.setTextColor(Color.RED);
            validPassword = false;
        }

        else if (!(stringPass1.matches(PASSWORDCHECK))) {
            checkThree.setText("Does not meet conditions");
            checkThree.setTextColor(Color.RED);
            validPassword = false;
        }
        else if (stringPass1.length() < 6 || stringPass1.length() > 30) {
            checkThree.setText("Invalid length");
            checkThree.setTextColor(Color.RED);
            validPassword = false;
        }

        else {
            checkThree.setText(getResources().getString(R.string.passCheck));
            checkThree.setTextColor(Color.LTGRAY);
            validPassword = true;
        }

        /**
         * Given all conditions are met, send info to database
         * On success, send to homepage; on fail, send error
         */
        if (validFullName && validEmail && validPassword) {

            AsyncHttpClient client = new AsyncHttpClient();
            Context context = this.getApplicationContext();
            JSONObject user;
            StringEntity entity;
            try {
                user = new JSONObject();
                user.put("fullName", stringFullName);
                user.put("email", stringEmail);
                user.put("password", stringPass1);
                user.put("confirmPassword", stringPass2);
                entity = new StringEntity(user.toString(), "UTF-8");
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
                    Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(i);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Context context = getApplicationContext();
                    CharSequence text;

                    if (statusCode == 406) {
                        text = "Email already used";
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
