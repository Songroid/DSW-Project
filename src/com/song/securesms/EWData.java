package com.song.securesms;

/**
 * This object is for EW items in the listview of EW dialog activity
 */
public class EWData {
    private String question;
    private char type;
    private int rbState = 2;

    public String getQuestion(){
        return question;
    }

    public void setQuestion(String question){
        this.question = question;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public int getRbState() {
        return rbState;
    }

    public void setRbState(int rbState) {
        this.rbState = rbState;
    }
}
