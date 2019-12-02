package com.example.tritonmeet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class HomeFragment extends Fragment {

    private FloatingActionButton addEventButton;
    private Intent retrieveCurrentUser;
    private String currentUser;

    private ExpandableListView expandableListView;
    private ScheduleExpandableListAdapter expandableListAdapter;
    private List<Integer> listEventNames;
    private HashMap<Integer, Event> listEvents = new HashMap<>();
    private int lastExpandedPosition = -1;
    private String myURL = "https://triton-meet.herokuapp.com/manageEvents";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        getActivity().setTitle("Schedule");

        expandableListView = view.findViewById(R.id.expListEvent);
        addEventButton = view.findViewById(R.id.fabAddEvent);

        retrieveCurrentUser = getActivity().getIntent();
        currentUser = retrieveCurrentUser.getStringExtra("email");

        AsyncHttpClient client = new AsyncHttpClient();
        JSONObject events;
        StringEntity entity;

        try {
            events = new JSONObject();
            events.put("email", currentUser);
            entity = new StringEntity(events.toString(), "UTF-8");
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("unexpected error", e);
        }

        client.get(getActivity().getApplicationContext(), myURL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String stringData = new String(responseBody);
                Log.i("Event JSON Object", stringData);
                JSONArray events;
                try {
                    events = new JSONArray(stringData);
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event;
                    int eventID;
                    String eventName;
                    String startDate;
                    String endDate;
                    String startTime;
                    String endTime;
                    JSONArray daysOfWeekJSON;
                    String daysOfWeek = "";
                    try {
                        event = events.getJSONObject(i);
                        eventID = event.getInt("eventID");
                        eventName = event.getString("eventName");
                        startDate = event.getString("startDate");
                        endDate = event.getString("endDate");
                        startTime = event.getString("startTime");
                        endTime = event.getString("endTime");
                        daysOfWeekJSON = event.getJSONArray("daysOfWeek");

                        for (int j = 0; j < daysOfWeekJSON.length(); j++) {
                            String day = daysOfWeekJSON.getString(j);
                            daysOfWeek += day + ",";
                        }
                    }
                    catch (JSONException e) {
                        throw new IllegalArgumentException("unexpected error", e);
                    }

                    if (daysOfWeek.length() > 0) {
                        daysOfWeek = daysOfWeek.substring(0, daysOfWeek.length() - 1);
                    }

                    listEvents.put(eventID, new Event(eventID, eventName, startDate, endDate, startTime, endTime, daysOfWeek));
                }

                HashMap<Integer, Event> sorted = sortByValue(listEvents);
                listEventNames = new ArrayList<>(sorted.keySet());
                expandableListAdapter = new ScheduleExpandableListAdapter(getActivity(), HomeFragment.this,currentUser, listEventNames, sorted);
                expandableListView.setAdapter(expandableListAdapter);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String data = new String(responseBody);
                Log.d("ERROR_MESSAGE FOR EVENT", data);
            }
        });

        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEvent(view);
            }
        });

        return view;
    }

    private void addEvent(View view) {
        Intent i = new Intent(getActivity(), AddEventActivity.class);
        i.putExtra("email", currentUser);
        startActivity(i);
    }

    private HashMap<Integer, Event> sortByValue(HashMap<Integer, Event> hm) {

        // Create a list from elements of HashMap
        List<Map.Entry<Integer, Event> > list = new LinkedList<>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Integer, Event> >() {
            public int compare(Map.Entry<Integer, Event> o1,
                               Map.Entry<Integer, Event> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<Integer, Event> temp = new LinkedHashMap<>();
        for (Map.Entry<Integer, Event> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}
