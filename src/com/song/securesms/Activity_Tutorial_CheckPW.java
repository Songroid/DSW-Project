package com.song.securesms;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * This is the tutorial screen for 'check password' activity
 * Check component details in layout file
 */
public class Activity_Tutorial_CheckPW extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_checkpw);

        // Touch to finish
        RelativeLayout tutorialLayer = (RelativeLayout) Activity_Tutorial_CheckPW.this.findViewById(R.id.rl_tutorial_checkpw);
        tutorialLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity_Tutorial_CheckPW.this.finish();
            }
        });

    }
}