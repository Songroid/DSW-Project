package com.song.securesms;

import java.util.HashMap;

/**
 * This object is for displaying a single message (combination of text messages)
 * in the listview of main activity
 */
public class SMSData {
    private String number;
	private String body;
    private String id;
    private String time;
    private Character type;
	private String sender;
    private String smsUniqueIDGroup;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Character getType() {
        return type;
    }

    public void setType(Character type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSmsUniqueIDGroup() {
        return smsUniqueIDGroup;
    }

    public void setSmsUniqueIDGroup(String smsUniqueIDGroup) {
        this.smsUniqueIDGroup = smsUniqueIDGroup;
    }
}
