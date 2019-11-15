package com.example.tritonmeet;

import java.util.ArrayList;

public class Group {

    private String groupName;
    private String owner;
    private ArrayList<String> members;

    public Group(String groupName, String owner) {
        this.groupName = groupName;
        this.owner = owner;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getOwner() {
        return owner;
    }
}
