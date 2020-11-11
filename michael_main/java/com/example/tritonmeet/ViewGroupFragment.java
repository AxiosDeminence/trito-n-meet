package com.example.tritonmeet;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class ViewGroupFragment extends Fragment {

    private ExpandableListView expandableListView;
    private FloatingActionButton addGroupEventButton;

    private String currentUser;
    private String groupName;
    private String ownerEmail;

    Context context;
    String myURL = "https://triton-meet.herokuapp.com/manageGroupEvents";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_viewgroup, container, false);
        Bundle arguments = getArguments();
        currentUser = arguments.getString("currentUser");
        groupName = arguments.getString("groupName");
        ownerEmail = arguments.getString("ownerEmail");
        getActivity().setTitle(groupName);

        expandableListView = view.findViewById(R.id.expListGroupEvent);
        addGroupEventButton = view.findViewById(R.id.fabAddGroupEvent);
        context = getActivity().getApplicationContext();

        addGroupEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addGroupEvent(v);
            }
        });

        return view;
    }

    private void addGroupEvent(View view) {
        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(getActivity());
        dialogGroup.setTitle("Create group event");
        final NumberPicker lengthPicker = new NumberPicker(getActivity());
        lengthPicker.setMinValue(1);
        lengthPicker.setMaxValue(12);
        final EditText eventName = new EditText(getActivity());
        eventName.setHint("Enter group name");
        eventName.setMaxEms(40);
        final TextView enterHours = new TextView(getActivity());
        enterHours.setText("Desired event length (hours)");
        enterHours.setTextSize(16);
        enterHours.setTextColor(Color.BLACK);
        enterHours.setTypeface(null, Typeface.BOLD);

        LinearLayout layout = new LinearLayout(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);
        layout.setPadding(2,2,2,2);

        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params2.setMargins(40,20,20,40);

        LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params3.leftMargin = 60;

        layout.addView(eventName, params2);
        layout.addView(enterHours, params3);
        layout.addView(lengthPicker, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dialogGroup.setView(layout);

        dialogGroup.setPositiveButton("Find Suggestions", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String nameOfEvent = eventName.getText().toString().trim();
                final int lengthOfEvent = lengthPicker.getValue();
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject infoSuggest;
                StringEntity entity;
                try {
                    infoSuggest = new JSONObject();
                    infoSuggest.put("owner", ownerEmail);
                    infoSuggest.put("groupName", groupName);
                    infoSuggest.put("lengthOfEvent", lengthOfEvent);

                    entity = new StringEntity(infoSuggest.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.get(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String stringData = new String(responseBody);
                        JSONArray suggestions;
                        try {
                            suggestions = new JSONArray(stringData);
                        }
                        catch (JSONException e) {
                            throw new IllegalArgumentException("unexpected error", e);
                        }

                        String[] suggestionArray = new String[suggestions.length()];
                        for (int i = 0; i < suggestions.length(); i++) {
                            String datetime;
                            try {
                                datetime = suggestions.getString(i);
                                suggestionArray[i] = datetime;
                            }
                            catch (JSONException e) {
                                throw new IllegalArgumentException("unexpected error", e);
                            }
                        }

                        AlertDialog.Builder choices = new AlertDialog.Builder(getActivity());
                        choices.setSingleChoiceItems(suggestionArray, 0, null);
                        choices.setPositiveButton("Pick", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                        choices.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        String data = new String(responseBody);
                        Log.d("ERRORMSG FOR SUGGEST 1", "Error " + statusCode + ": " + error);
                        Log.d("ERRORMSG FOR SUGGEST 2", data);
                        System.out.println(ownerEmail);
                        System.out.println(groupName);
                        System.out.println(lengthOfEvent);
                    }
                });
            }
        });

        dialogGroup.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialogGroup.show();
    }
}
