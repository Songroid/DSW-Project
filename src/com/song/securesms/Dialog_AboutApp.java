package com.song.securesms;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * This is the dialog window for app information
 * Location: the second row of navigation drawer
 */
public class Dialog_AboutApp extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title bar of dialog
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.dialog_aboutapp);

        Button aboutAppButton = (Button) findViewById(R.id.aboutappButton);
        aboutAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog_AboutApp.this.finish();
            }
        });
    }
}