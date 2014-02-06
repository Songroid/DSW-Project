package com.song.securesms;

/**
 * Created by song_jin on 7/24/13.
 */
public class VZWKey {
    private int id;
    private int index;

    public VZWKey(Integer id, Integer index) {
        this.id = id;
        this.index = index;
    }

    @Override
    public String toString() {
        return "["+id+"/"+index+"]";
    }
}
