package com.song.securesms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Custom list adapter for drawer items
 */
public class DrawerListAdapter extends ArrayAdapter {

    private final Context context;
    private final List<DrawerData> drawerList;

    public DrawerListAdapter(Context context, List<DrawerData> drawerList) {
        super(context, R.layout.activity_main, drawerList);
        this.context = context;
        this.drawerList = drawerList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View drawerView = inflater.inflate(R.layout.drawer_listadapter, parent, false);

        TextView drawer_content = (TextView) drawerView.findViewById(R.id.drawer_content);
        drawer_content.setText(drawerList.get(position).getContent());

        ImageView drawer_image = (ImageView) drawerView.findViewById(R.id.drawer_image);

        Character c = drawerList.get(position).getImage();
        switch (c){
            // tutorial
            case 't':
                drawer_image.setImageResource(R.drawable.drawer_help);
                break;
            // about the app
            case 'a':
                drawer_image.setImageResource(R.drawable.drawer_about);
                break;
        }

        return drawerView;
    }
}
