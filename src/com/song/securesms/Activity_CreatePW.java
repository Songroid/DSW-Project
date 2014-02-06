package com.song.securesms;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity is for the passphrase creation screen. The components used
 * are nearly the same with the Activity_CheckPW, except for the menu button and
 * tutorial screen.
 */
public class Activity_CreatePW extends Activity {
	String passphrase = "";
    private int carrier;
	HandleKey func;
	private boolean doubleBackToExitPressedOnce = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registry);

        // each carrier has a different value for later usage of parsing text messages
        TelephonyManager manager = (TelephonyManager)Activity_CreatePW.this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getNetworkOperatorName().equals("Verizon Wireless")){
            carrier = 1;
        } else if (manager.getNetworkOperatorName().equals("AT&T")){
            carrier = 2;
        }
        // instantiate helper class by assigning corresponding carrier
        func = new HandleKey(Activity_CreatePW.this, carrier);
				
		class ButtonListener implements android.view.View.OnClickListener{
			public void onClick(View v) {
				View parent = (View)v.getParent();

				// get passphrase from user input
				EditText pw = (EditText) parent.findViewById(R.id.pw_input2);
                String password = pw.getText().toString();
				StringBuilder passBuilder = new StringBuilder(password);

                if(password.matches("")){
                    Toast.makeText(Activity_CreatePW.this, "Please enter at least one character.", Toast.LENGTH_SHORT).show();
                    return;
                }
				
				// buffer the passphrase.
                // if it is longer than 16 bytes, only use the first 16 bytes
				if(passBuilder.length()>=16){
					passphrase=passBuilder.substring(0,16);
				}
				// if it is shorter than 16 bytes, add "0" as padding til it is 16 bytes.
				else{
					for(int i=passBuilder.length();i<16;i++){
						passBuilder.append("0");
					}
					passphrase=passBuilder.toString();
				}
				
				try {
					// MD5 digest is created here
					// display passphrase created via toast
					createMD5(passphrase);
					Toast.makeText(Activity_CreatePW.this, "Passphrase created", Toast.LENGTH_SHORT).show();
					
					// add the passphrase into the intent
					// send the passphrase to Activity_Main for encrypting private key
					Intent intent = new Intent(Activity_CreatePW.this, Activity_Main.class);
					intent.putExtra("passkey",passphrase);
					Activity_CreatePW.this.startActivity(intent);
					Activity_CreatePW.this.finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                } catch (IOException e) {
					Toast.makeText(Activity_CreatePW.this, "IOException", Toast.LENGTH_SHORT).show();
				}
			}
		}
		
		// perform click function
		Button finish = (Button) this.findViewById(R.id.button2);
		finish.setOnClickListener(new ButtonListener());
		
		// hide soft keyboard after clicking outside EditText
		View view = findViewById(android.R.id.content);
		if(!(view instanceof EditText)){
			view.setOnTouchListener(new OnTouchListener(){

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					hideSoftKeyboard(Activity_CreatePW.this);
					return false;
				}
				
			});
		}

        // if clicked, display password
        CheckBox showPW = (CheckBox) findViewById(R.id.showPW_create);
        showPW.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            CheckBox showPW = (CheckBox) findViewById(R.id.showPW_create);
            EditText inputPW = (EditText) findViewById(R.id.pw_input2);
            if (showPW.isChecked()){
                inputPW.setInputType(InputType.TYPE_CLASS_TEXT);
            } else {
                inputPW.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            }
        });
	}
	
	@Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast toast = new Toast(Activity_CreatePW.this);
        toast.setDuration(Toast.LENGTH_SHORT);

        LayoutInflater inflater = (LayoutInflater) Activity_CreatePW.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast_message, null);
        TextView backAgain = (TextView) view.findViewById(R.id.toast_content);
        backAgain.setText("Please click BACK again to exit");
        toast.setView(view);
        toast.show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
             doubleBackToExitPressedOnce=false;   

            }
        }, 2000);
    } 
	
	public void createMD5(String passphrase) throws IOException {
		String md5content = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			// passphrase: String -> byte[] -> MD5 encryption -> HexString
			byte[] bytesofmsg = passphrase.getBytes("UTF-8");
			byte[] thedigest = md.digest(bytesofmsg);
			md5content = func.asHex(thedigest);
			saveGeneral("passphrase.md",md5content);
		} catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
	}
	
	// general method to save a file in the internal storage of Android
    public void saveGeneral(String fileName, String content) throws IOException{
        File file = new File(getApplicationContext().getFilesDir(), fileName);
    	ObjectOutputStream outMD5 = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    	outMD5.writeObject(content);
    	outMD5.close();
    }
    
    public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

}
