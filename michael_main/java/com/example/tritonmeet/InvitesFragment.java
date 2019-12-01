package com.example.tritonmeet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class InvitesFragment extends Fragment {

    private ListView listView;
    private InvitesListAdapter listAdapter;
    private List<String> listOfInvites;
    private HashMap<String, Group> hashGroup = new HashMap<>();
    private String myURL = "https://triton-meet.herokuapp.com/manageGroups";

    private Intent retrieveCurrentUser;
    private String currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invites, container, false);
        getActivity().setTitle("Manage Invites");

        retrieveCurrentUser = getActivity().getIntent();
        currentUser = retrieveCurrentUser.getStringExtra("email");

        listView = view.findViewById(R.id.listGroup);

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
                Log.i("Invites JSON Object", stringData);

                JSONObject objectData;
                JSONArray invites;
                try {
                    objectData = new JSONObject(stringData);
                    invites = objectData.getJSONArray("invites");

                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                for (int i = 0; i < invites.length(); i++) {

                    JSONObject invite;
                    String nameOfGroup;
                    String nameOfOwner;
                    try {
                        invite = invites.getJSONObject(i);
                        nameOfGroup = invite.getString("groupName");
                        nameOfOwner = invite.getString("owner");
                        System.out.println(invite.toString());
                    } catch (JSONException e) {
                        throw new IllegalArgumentException("unexpected error", e);
                    }

                    hashGroup.put(nameOfGroup, new Group(nameOfGroup, nameOfOwner, getActivity()));
                }
                listOfInvites = new ArrayList<>(hashGroup.keySet());
                listAdapter = new InvitesListAdapter(getActivity(), currentUser, listOfInvites, hashGroup);
                listView.setAdapter(listAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String data = new String(responseBody);
                Log.d("ERROR_MESSAGE", data);
            }
        });

        return view;
    }
}
