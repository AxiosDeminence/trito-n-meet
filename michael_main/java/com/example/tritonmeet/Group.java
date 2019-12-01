package com.example.tritonmeet;

import android.content.Context;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class Group {

    private String groupName;
    private String owner;
    private ArrayList<String> members;
    private Context context;
    private String fullName;

    public Group(String groupName, String owner, Context context) {
        this.groupName = groupName;
        this.owner = owner;
        members = new ArrayList<>();
        this.context = context;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getOwner() {
        return owner;
    }

    public String getOwnerFullName() {

        SyncHttpClient client = new SyncHttpClient();
        JSONObject user;
        StringEntity entity;

        try {
            user = new JSONObject();
            user.put("email", getOwner());
            entity = new StringEntity(user.toString(), "UTF-8");
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("unexpected error", e);
        }

        client.get(context, "https://triton-meet.herokuapp.com/getFullName", entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    fullName = response.getString("name");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable error, JSONObject errorResponse) {
                fullName = "Error " + statusCode;
            }
        });
        return fullName;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public void addMember(String memberEmail) {
        members.add(memberEmail);
    }
}
