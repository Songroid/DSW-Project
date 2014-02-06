package com.song.securesms;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * This is a tutorial screen implementation
 * Check component details in layout file
 */
public class Activity_Tutorial extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Touch to finish
        RelativeLayout tutorialLayer = (RelativeLayout) Activity_Tutorial.this.findViewById(R.id.rl_tutorial);
        tutorialLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity_Tutorial.this.finish();
            }
        });
    }
}