package com.song.securesms;

import java.util.HashMap;

/**
 * This object is for setting an individual text message
 */
public class VZWData {
    private int index;
    private int blocks;
    private int id;
    private String number;
    private String time = "";
    private String content;
    private String uniqueID;
    private char type = ' ';
    private String smsUniqueIDGroup;
    private int ttl;
    private boolean shouldDelete = false;

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    public String getSmsUniqueIDGroup() {
        return smsUniqueIDGroup;
    }

    public void setSmsUniqueIDGroup(String smsUniqueIDGroup) {
        this.smsUniqueIDGroup = smsUniqueIDGroup;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public boolean isShouldDelete() {
        return shouldDelete;
    }

    public void setShouldDelete(boolean shouldDelete) {
        this.shouldDelete = shouldDelete;
    }
}
