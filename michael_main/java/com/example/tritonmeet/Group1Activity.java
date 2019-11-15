package com.example.tritonmeet;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Group1Activity extends AppCompatActivity {

    FloatingActionButton addGroupButton;
    String groupName;
    ExpandableListView expandableListView;
    CustomExpandableListAdapter expandableListAdapter;
    List<String> expandableListNames;
    HashMap<String, Group> listGroups = new HashMap<>();
    private int lastExpandedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group1);

        expandableListView = findViewById(R.id.expListGroup);
        addGroupButton = findViewById(R.id.fabAddGroup);

        expandableListNames = new ArrayList<>(listGroups.keySet());

        expandableListAdapter = new CustomExpandableListAdapter(Group1Activity.this, expandableListNames, listGroups);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createGroup(view);
            }
        });
    }

    public void createGroup(View view) {

        /**
         * Creates necessary elements of the dialog
         */
        AlertDialog.Builder dialogGroup = new AlertDialog.Builder(Group1Activity.this);
        dialogGroup.setTitle("Create group");
        final EditText editGroupName = new EditText(Group1Activity.this);
        editGroupName.setHint("Group name");
        editGroupName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        /**
         * Creates padding for the dialog (for aesthetic)
         */
        final int AMOUNT_PADDING = 20;
        editGroupName.setSingleLine();
        FrameLayout container = new FrameLayout(Group1Activity.this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = convertDpToPx(AMOUNT_PADDING);
        params.bottomMargin = convertDpToPx(AMOUNT_PADDING);
        params.leftMargin = convertDpToPx(AMOUNT_PADDING);
        params.rightMargin = convertDpToPx(AMOUNT_PADDING);
        editGroupName.setLayoutParams(params);
        container.addView(editGroupName);

        dialogGroup.setView(container);

        /**
         * Create group in database if they click "create"
         */
        dialogGroup.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                groupName = editGroupName.getText().toString().trim();
                Intent retrieveOwner = getIntent();
                String owner = retrieveOwner.getStringExtra("email");

                expandableListNames.add(groupName);
                listGroups.put(groupName, new Group(groupName, owner));
                expandableListAdapter.updateData(expandableListNames, listGroups);
            }
        });

        /**
         * Exit dialog if they click cancel
         */
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
    public static int convertDpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
