package com.example.tritonmeet;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class ScheduleExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private HomeFragment fragment;
    private String currentUser;
    private List<Integer> eventList;
    private HashMap<Integer, Event> expandableList;
    private String myURL = "https://triton-meet.herokuapp.com/manageEvents";

    public ScheduleExpandableListAdapter(Context context,
                                         HomeFragment fragment,
                                         String currentUser,
                                         List<Integer> eventList,
                                         HashMap<Integer, Event> expandableList) {
        this.context = context;
        this.fragment = fragment;
        this.currentUser = currentUser;
        this.eventList = eventList;
        this.expandableList = expandableList;
    }

    @Override
    public int getGroupCount() {
        return eventList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return eventList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return expandableList.get(eventList.get(groupPosition));
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Event event = (Event) getChild(groupPosition, 0);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.eventparent, null);
        }

        TextView eventName = convertView.findViewById(R.id.eventName);
        TextView eventDate = convertView.findViewById(R.id.eventDate);
        TextView eventTime = convertView.findViewById(R.id.eventTime);
        TextView repeating = convertView.findViewById(R.id.repeating);

        eventName.setText(event.getEventName());
        if (event.getStartDate().equals(event.getEndDate())) {
            String date1 = event.getStartDate();
            eventDate.setText(date1);
        }
        else {
            String date2 = event.getStartDate() + " - " + event.getEndDate();
            eventDate.setText(date2);
        }

        String time =  Event.time24to12(event.getStartTime()) + " - " + Event.time24to12(event.getEndTime());
        eventTime.setText(time);

        if (event.getStartDate().equals(event.getEndDate())) {
            repeating.setText("One day event");
        }
        else {
            if (event.getWeekly().length() == 56) {
                repeating.setText("Daily");
            }
            else if (event.getWeekly().length() == 0) {
                repeating.setText("One time event");
            }
            else {
                String[] weekly = event.getWeekly().split(",", 0);
                String weeklyString = "";

                for (String s : weekly) {
                    weeklyString += s + ", ";
                }
                repeating.setText(weeklyString.substring(0, weeklyString.length() - 2));
            }
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final Event event = (Event) getChild(groupPosition, 0);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.eventchild, null);
        }

        Button deleteButton = convertView.findViewById(R.id.deleteEventButton);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete(event, v);
            }
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private void delete(Event event, View view) {
        final String eventName = event.getEventName();
        final int eventID = event.getEventID();

        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(context);
        dialogGroup.setTitle("Confirm");
        dialogGroup.setMessage("Are you sure you want to delete " + eventName + "?");
        dialogGroup.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject deleteGroup;
                StringEntity entity;
                try {
                    deleteGroup = new JSONObject();
                    deleteGroup.put("action", "delete");
                    deleteGroup.put("email", currentUser);
                    deleteGroup.put("eventID", eventID);
                    entity = new StringEntity(deleteGroup.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "You have deleted " + eventName;
                        Toast.makeText(context, message, duration).show();
                        FragmentTransaction ft = fragment.getFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment_container, new HomeFragment()).commit();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "Error " + statusCode + ": Could not delete";
                        Toast.makeText(context, message, duration).show();
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
