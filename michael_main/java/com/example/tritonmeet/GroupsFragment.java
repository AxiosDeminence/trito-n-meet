package com.example.tritonmeet;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class GroupsFragment extends Fragment {

    private FloatingActionButton addGroupButton;
    private String groupName;
    private ExpandableListView expandableListView;
    private CustomExpandableListAdapter expandableListAdapter;
    private List<String> expandableListNames;
    private HashMap<String, Group> listGroups = new HashMap<>();
    private int lastExpandedPosition = -1;
    private String myURL = "https://triton-meet.herokuapp.com/manageGroups";

    private Intent retrieveCurrentUser;
    private String currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        getActivity().setTitle("Groups");

        retrieveCurrentUser = getActivity().getIntent();
        currentUser = retrieveCurrentUser.getStringExtra("email");

        expandableListView = view.findViewById(R.id.expListGroup);
        addGroupButton = view.findViewById(R.id.fabAddGroup);

        // Give JSON Object of logged in user's email, and get groups that they're in
        AsyncHttpClient client = new AsyncHttpClient();
        JSONObject user;
        StringEntity entity;

        try {
            user = new JSONObject();
            user.put("email", currentUser);
            entity = new StringEntity(user.toString(), "UTF-8");
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("unexpected error", e);
        }

        client.get(getActivity().getApplicationContext(), myURL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                // Parse JSON Object and populate hash map
                String stringData = new String(responseBody);

                JSONObject objectData;
                JSONArray groups;
                try {
                    objectData = new JSONObject(stringData);
                    groups = objectData.getJSONArray("groups");

                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                for (int i = 0; i < groups.length(); i++) {

                    JSONObject group;
                    String nameOfGroup;
                    String nameOfOwner;
                    try {
                        group = groups.getJSONObject(i);
                        nameOfGroup = group.getString("groupName");
                        nameOfOwner = group.getString("owner");
                    }
                    catch (JSONException e) {
                        throw new IllegalArgumentException("unexpected error", e);
                    }

                    listGroups.put(nameOfGroup, new Group(nameOfGroup, nameOfOwner, getActivity()));

                }

                expandableListNames = new ArrayList<>(listGroups.keySet());
                expandableListAdapter = new CustomExpandableListAdapter(getActivity(), GroupsFragment.this,currentUser, expandableListNames, listGroups);
                expandableListView.setAdapter(expandableListAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String data = new String(responseBody);
                Log.d("ERROR_MESSAGE", data);
            }
        });

        // Allow expandable list view to expand and show buttons
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

        // Allow user to create groups
        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createGroup(view);
            }
        });

        return view;
    }

    private void createGroup(View view) {

        // Creates necessary elements of the dialog with input limit of length 30
        final int MAX_LENGTH = 30;
        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(getActivity());
        dialogGroup.setTitle("Create group");
        final EditText editGroupName = new EditText(getActivity());
        editGroupName.setHint("Group name");
        editGroupName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(MAX_LENGTH);
        editGroupName.setFilters(FilterArray);

        // Creates padding for the dialog (for aesthetic)
        final int AMOUNT_PADDING = 20;
        editGroupName.setSingleLine();
        FrameLayout container = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = convertDpToPx(AMOUNT_PADDING);
        params.bottomMargin = convertDpToPx(AMOUNT_PADDING);
        params.leftMargin = convertDpToPx(AMOUNT_PADDING);
        params.rightMargin = convertDpToPx(AMOUNT_PADDING);
        editGroupName.setLayoutParams(params);
        container.addView(editGroupName);

        dialogGroup.setView(container);

        // Create group in database if they click "create"
        dialogGroup.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                groupName = editGroupName.getText().toString().trim();

                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject group;
                StringEntity entity;
                try {
                    group = new JSONObject();
                    group.put("action", "create");
                    group.put("groupName", groupName);
                    group.put("owner", currentUser);
                    entity = new StringEntity(group.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(getActivity().getApplicationContext(), myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "Success!";
                        Toast.makeText(getActivity().getApplicationContext(), message, duration).show();

                        // Refresh page so that it updates when you create group
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.detach(GroupsFragment.this).attach(GroupsFragment.this).commit();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence text;

                        if (statusCode == 400) {
                            text = "Invalid group";
                        }
                        else {
                            text = "Error " + statusCode + ": " + error;
                        }
                        Toast.makeText(getActivity().getApplicationContext(), text, duration).show();
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

    /**
     * Convert density of pixels into pixels
     * @param dp Density of pixels
     * @return Pixels
     */
    private static int convertDpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
