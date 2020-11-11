package com.example.tritonmeet;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import java.util.HashMap;
import java.util.List;

public class GroupScheduleExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private ViewGroupFragment fragment;
    private Group group;
    private List<Integer> eventList;
    private HashMap<Integer, Event> expandableList;

    public GroupScheduleExpandableListAdapter(Context context,
                                         ViewGroupFragment fragment,
                                         Group group,
                                         List<Integer> eventList,
                                         HashMap<Integer, Event> expandableList) {
        this.context = context;
        this.fragment = fragment;
        this.group = group;
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
        Group group = (Group) getGroup(groupPosition);

        return null;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Event event = (Event) getChild(groupPosition, 0);
        Group group = (Group) getGroup(groupPosition);
        return null;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
