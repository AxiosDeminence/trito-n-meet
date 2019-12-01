package com.example.tritonmeet;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class GroupsExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private GroupsFragment fragment;
    private String currentUser;
    private List<String> groupList;
    private HashMap<String, Group> expandableList;
    private String myURL = "https://triton-meet.herokuapp.com/manageGroups";

    public GroupsExpandableListAdapter(Context context,
                                       GroupsFragment fragment,
                                       String currentUser,
                                       List<String> groupList,
                                       HashMap<String, Group> expandableList) {
        this.context = context;
        this.fragment = fragment;
        this.currentUser = currentUser;
        this.groupList = groupList;
        this.expandableList = expandableList;
    }

    @Override
    public int getGroupCount() {
        return groupList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return expandableList.get(groupList.get(groupPosition));
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
    public int getChildTypeCount() {
        return 2;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        Group group = (Group) getChild(groupPosition, 0);
        if (group.getOwner().equals(currentUser)) {
            return 0;
        }
        else {
            return 1;
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        Group group = (Group) getChild(groupPosition, 0);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.group1parent, null);
        }

        TextView groupName = convertView.findViewById(R.id.groupName);
        TextView owner = convertView.findViewById(R.id.owner);

        groupName.setText(group.getGroupName());
        owner.setText("Owner: " + group.getOwner());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final Group group = (Group) getChild(groupPosition, 0);
        Button viewButton = null;
        Button inviteButton = null;
        Button leaveButton = null;
        Button deleteButton = null;
        int val;
        if (group.getOwner().equals(currentUser)) {
            val = 0;
        }
        else {
            val = 1;
        }

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (val == 0) {
                convertView = layoutInflater.inflate(R.layout.group1child, null);
                viewButton = convertView.findViewById(R.id.viewButton1);
                inviteButton = convertView.findViewById(R.id.inviteButton1);
                leaveButton = convertView.findViewById(R.id.leaveButton1);
                deleteButton = convertView.findViewById(R.id.deleteButton1);
            }
            else {
                convertView = layoutInflater.inflate(R.layout.group1child2, null);
                viewButton = convertView.findViewById(R.id.viewButton1);
                inviteButton = convertView.findViewById(R.id.inviteButton1);
                leaveButton = convertView.findViewById(R.id.leaveButton1);
            }
        }

        if (viewButton != null) {
            viewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    view(group, v);
                }
            });
        }
        if (inviteButton != null) {
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    invite(group, v);
                }
            });
        }
        if (leaveButton != null) {
            leaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    leave(group, v);
                }
            });
        }
        if (deleteButton != null) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delete(group, v);
                }
            });
        }

        Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        convertView.startAnimation(animation);

        return convertView;

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private void view(Group group, View v) {

    }

    private void invite(Group group, View v) {
        final String groupName = group.getGroupName();
        final String ownerEmail = group.getOwner();

        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(context);
        dialogGroup.setTitle("List emails (comma separated)");
        final EditText inviter = new EditText(context);
        inviter.setHint("E.g a@ucsd.edu,b@ucsd.edu");
        inviter.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        // Creates padding for the dialog (for aesthetic)
        final int AMOUNT_PADDING = 20;
        inviter.setSingleLine();
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = convertDpToPx(AMOUNT_PADDING);
        params.bottomMargin = convertDpToPx(AMOUNT_PADDING);
        params.leftMargin = convertDpToPx(AMOUNT_PADDING);
        params.rightMargin = convertDpToPx(AMOUNT_PADDING);
        inviter.setLayoutParams(params);
        container.addView(inviter);

        dialogGroup.setView(container);

        dialogGroup.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String stringInviteList = inviter.getText().toString().trim();

                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject inviteList;
                StringEntity entity;
                try {
                    inviteList = new JSONObject();
                    inviteList.put("action", "invite");
                    inviteList.put("groupName", groupName);
                    inviteList.put("owner", ownerEmail);
                    inviteList.put("users", stringInviteList);
                    entity = new StringEntity(inviteList.toString(), "UTF-8");
                    System.out.println(inviteList.toString());
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "Invitation sent";
                        Toast.makeText(context, message, duration).show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "Invitation failed - " + statusCode + ": " + error;
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

    private void leave(Group group, View v) {
        final String groupName = group.getGroupName();
        final String ownerEmail = group.getOwner();

        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(context);
        dialogGroup.setTitle("Confirm");
        dialogGroup.setMessage("Are you sure you want to leave " + groupName + "?");
        dialogGroup.setPositiveButton("Leave", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject leaveGroup;
                StringEntity entity;
                try {
                    leaveGroup = new JSONObject();
                    leaveGroup.put("action", "remove");
                    leaveGroup.put("groupName", groupName);
                    leaveGroup.put("owner", ownerEmail);
                    leaveGroup.put("email", currentUser);
                    entity = new StringEntity(leaveGroup.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "You have left " + groupName;
                        Toast.makeText(context, message, duration).show();
                        FragmentTransaction ft = fragment.getFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment_container, new GroupsFragment()).commit();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "Error " + statusCode + ": Could not leave";
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

    private void delete(Group group, View v) {
        final String groupName = group.getGroupName();
        final String ownerEmail = group.getOwner();

        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(context);
        dialogGroup.setTitle("Confirm");
        dialogGroup.setMessage("Are you sure you want to delete " + groupName + "?");
        dialogGroup.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AsyncHttpClient client = new AsyncHttpClient();

                JSONObject deleteGroup;
                StringEntity entity;
                try {
                    deleteGroup = new JSONObject();
                    deleteGroup.put("action", "delete");
                    deleteGroup.put("groupName", groupName);
                    deleteGroup.put("owner", ownerEmail);
                    entity = new StringEntity(deleteGroup.toString(), "UTF-8");
                }
                catch (JSONException e) {
                    throw new IllegalArgumentException("unexpected error", e);
                }

                client.post(context, myURL, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        int duration = Toast.LENGTH_SHORT;
                        CharSequence message = "You have deleted " + groupName;
                        Toast.makeText(context, message, duration).show();
                        FragmentTransaction ft = fragment.getFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment_container, new GroupsFragment()).commit();
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

    /**
     * Convert density of pixels into pixels
     * @param dp Density of pixels
     * @return Pixels
     */
    private static int convertDpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
