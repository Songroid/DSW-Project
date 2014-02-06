// this activity is for initialized screen

package com.song.securesms;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * This activity is only for displaying a logo screen for a fixed
 * period of time
 */
public class Activity_Splash extends Activity {

	private static String TAG = Activity_Splash.class.getName();
	private static long SLEEP_TIME = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int state = 0;
        // enable full-screen and no action bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set background color
		View view = this.getWindow().getDecorView();
	    view.setBackgroundColor(Color.parseColor("#ffffff"));
	    
	    // start timer and launch main activity
	 	IntentLauncher launcher = new IntentLauncher();
	 	if(state==0){
	 		launcher.start();
	 	}
		
		setContentView(R.layout.activity_splash);
	}
	
	private class IntentLauncher extends Thread {
		@Override
		public void run(){
			try {
				Thread.sleep(SLEEP_TIME*500);
			} catch (Exception e){
				e.printStackTrace();
			} finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // check if md5 digest file exists -> passphrase existed
                        File file = new File(getApplicationContext().getFilesDir(), "passphrase.md");
                        // if the passphrase has been created, go to check password activity
                        if(file.exists()){
                            Intent intent = new Intent(Activity_Splash.this, Activity_CheckPW.class);
                            Activity_Splash.this.startActivity(intent);
                            Activity_Splash.this.finish();
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        }
                        // if the passphrase doesn't exist, go to create password activity
                        else{
                            Intent intent = new Intent(Activity_Splash.this, Activity_CreatePW.class);
                            Activity_Splash.this.startActivity(intent);
                            Activity_Splash.this.finish();
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        }
                    }
                });
            }
		}
	}
}
