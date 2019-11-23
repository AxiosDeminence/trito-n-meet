package com.example.tritonmeet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class CustomListAdapter extends BaseAdapter {

    private Context context;
    private String currentUser;
    private List<String> listOfInvites;
    private HashMap<String, Group> hashGroup;
    private String myURL = "https://triton-meet.herokuapp.com/manageGroups";

    public CustomListAdapter(Context context,
                             String currentUser,
                             List<String> listOfInvites,
                             HashMap<String, Group> hashGroup) {
        this.context = context;
        this.currentUser = currentUser;
        this.listOfInvites = listOfInvites;
        this.hashGroup = hashGroup;
    }

    @Override
    public int getCount() {
        return listOfInvites.size();
    }

    @Override
    public Object getItem(int position) {
        return listOfInvites.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final String groupName = listOfInvites.get(position);
        Group group = hashGroup.get(groupName);
        final String owner = group.getOwner();

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.inviteparent, null);
        }

        TextView groupText = convertView.findViewById(R.id.groupNameInvite);
        TextView ownerText = convertView.findViewById(R.id.ownerInvite);
        Button acceptButton = convertView.findViewById(R.id.acceptButton);
        Button declineButton = convertView.findViewById(R.id.declineButton);

        groupText.setText(groupName);
        ownerText.setText(group.getOwner());

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject acceptObject;
                StringEntity entity;
                try {
                    acceptObject = new JSONObject();
                    acceptObject.put("action", "join");
                    acceptObject.put("groupName", groupName);
                    acceptObject.put("owner", owner);
                    acceptObject.put("email", currentUser);
                    entity = new StringEntity(acceptObject.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        listOfInvites.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Invite accepted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        String text = "Error " + statusCode + ": " + error;
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject declineObject;
                StringEntity entity;
                try {
                    declineObject = new JSONObject();
                    declineObject.put("action", "remove");
                    declineObject.put("groupName", groupName);
                    declineObject.put("owner", owner);
                    declineObject.put("email", currentUser);
                    entity = new StringEntity(declineObject.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        listOfInvites.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Invite declined", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        String text = "Error " + statusCode + ": " + error;
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        return convertView;
    }
}
