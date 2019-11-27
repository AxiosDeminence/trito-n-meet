package com.example.tritonmeet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Event implements Comparable<Event> {

    private int eventID;
    private String eventName;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String weekly;

    public Event(int eventID, String eventName, String startDate, String endDate,
                 String startTime, String endTime, String weekly) {
        this.eventID = eventID;
        this.eventName = eventName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.weekly = weekly;
    }

    public int getEventID() {
        return eventID;
    }

    public String getEventName() {
        return eventName;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getWeekly() {
        return weekly;
    }

    public static String time12to24(String time) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
        String time24 = time;
        try {
            time24 = displayFormat.format(parseFormat.parse(time));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return time24;
    }

    public static String time24to12(String time) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("hh:mm a");
        SimpleDateFormat parseFormat = new SimpleDateFormat("HH:mm");
        String time12 = time;
        try {
            time12 = displayFormat.format(parseFormat.parse(time));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return time12;
    }

    public int compareTo(Event other) {
        String stringDate1 = getStartDate();
        String time1 = getStartTime();
        String stringDate2 = other.getStartDate();
        String time2 = other.getStartTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = sdf.parse(stringDate1 + " " + time1);
            date2 = sdf.parse(stringDate2 + " " + time2);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return date1.compareTo(date2);
    }

}
