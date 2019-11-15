package com.example.tritonmeet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> groupList;
    private HashMap<String, Group> expandableList;

    public CustomExpandableListAdapter(Context context,
                                       List<String> groupList,
                                       HashMap<String, Group> expandableList) {
        this.context = context;
        this.groupList = groupList;
        this.expandableList = expandableList;
    }

    public void updateData(List<String> groupList, HashMap<String, Group> expandableList) {
        this.expandableList = expandableList;
        this.groupList = groupList;
        this.notifyDataSetChanged();
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
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String name = (String) getGroup(groupPosition);
        Group group = (Group) getChild(groupPosition, 0);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.group1parent, null);
        }

        TextView groupName = convertView.findViewById(R.id.groupName);
        TextView owner = convertView.findViewById(R.id.owner);

        groupName.setText(name);
        owner.setText("Owner: " + group.getOwner());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.group1child, null);
        }

        Button viewButton = convertView.findViewById(R.id.viewButton);
        Button inviteButton = convertView.findViewById(R.id.inviteButton);
        Button leaveButton = convertView.findViewById(R.id.leaveButton);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view();
                Toast.makeText(v.getContext(), "View", Toast.LENGTH_SHORT);
            }
        });

        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invite();
                Toast.makeText(v.getContext(), "Invite", Toast.LENGTH_SHORT);
            }
        });

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leave();
                Toast.makeText(v.getContext(), "Leave", Toast.LENGTH_SHORT);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete();
                Toast.makeText(v.getContext(), "Delete", Toast.LENGTH_SHORT);
            }
        });

        Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        convertView.startAnimation(animation);

        return convertView;

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**
     * TODO
     */
    public void view() {
    }

    public void invite() {

    }

    public void leave() {

    }

    public void delete() {

    }
}
