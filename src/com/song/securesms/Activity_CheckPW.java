package com.song.securesms;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity is for the passphrase input screen. It provides a tutorial button in
 * the action bar, an edittext for passphrase input, a submit button and a remove app
 * clickable textview.
 */
public class Activity_CheckPW extends Activity {
	HandleKey func;
	String passphrase = "";
    private int carrier;
	private boolean doubleBackToExitPressedOnce = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_pw);

        // each carrier has a different value for later usage of parsing text messages
        TelephonyManager manager = (TelephonyManager)Activity_CheckPW.this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getNetworkOperatorName().equals("Verizon Wireless")){
            carrier = 1;
        } else if (manager.getNetworkOperatorName().equals("AT&T")){
            carrier = 2;
        } else if (manager.getNetworkOperatorName().equals("T-Mobile")){
            carrier = 4;
        }

        // instantiate helper class by assigning corresponding carrier
        func = new HandleKey(Activity_CheckPW.this, carrier);

		// check passphrase (for submit button)
		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				View parent = (View)v.getParent();
				
				// get passphrase from user input
				final EditText pw = (EditText) parent.findViewById(R.id.checkpw_input);
				StringBuilder passBuilder = new StringBuilder(pw.getText().toString());

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
				
				// create MD5 file of the input passphrase
				String md5content;
				try {
					MessageDigest md = MessageDigest.getInstance("Md5");
					byte[] bytesofpw = passphrase.getBytes("UTF-8");
					byte[] thedigest = md.digest(bytesofpw);
					md5content = func.asHex(thedigest);
					
					// generate a new MD5 and compare it with the saved one
					if(compareMD5("passphrase.md", md5content)){
						// add the passphrase into the intent
						// send the passphrase to Activity_Main for encrypting private key
						Intent intent = new Intent(Activity_CheckPW.this, Activity_Main.class);
						intent.putExtra("passkey",passphrase);
						Activity_CheckPW.this.startActivity(intent);
						Activity_CheckPW.this.finish();
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					}
					else{
                        Toast toast = new Toast(Activity_CheckPW.this);
                        toast.setDuration(Toast.LENGTH_SHORT);

                        // this block is for custom toast (color purple)
                        LayoutInflater inflater = (LayoutInflater) Activity_CheckPW.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.toast_message, null);
                        TextView wrongPW = (TextView) view.findViewById(R.id.toast_content);
                        wrongPW.setText("Wrong password");
                        toast.setView(view);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
						pw.setText("");
					}
					
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		});
		
		// hide soft keyboard when click the view other than EditText
		View view = findViewById(android.R.id.content);
		if(!(view instanceof EditText)){
			view.setOnTouchListener(new OnTouchListener(){

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					hideSoftKeyboard(Activity_CheckPW.this);
					return false;
				}
				
			});
		}

        // if the checkbox is clicked, display the input passphrase from dot to text
        CheckBox showPW = (CheckBox) findViewById(R.id.showPW);
        showPW.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                CheckBox showPW = (CheckBox) findViewById(R.id.showPW);
                EditText inputPW = (EditText) findViewById(R.id.checkpw_input);
                if (showPW.isChecked()){
                    inputPW.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    inputPW.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        // if the user forgot the passphrase, there're two actions on the clickable textview:
        // click: a toast will be popped up to notify the user
        // long click: redirect to system uninstallation window
        TextView forgot = (TextView) findViewById(R.id.forgotPW);

        forgot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = new Toast(Activity_CheckPW.this);
                toast.setDuration(Toast.LENGTH_SHORT);

                LayoutInflater inflater = (LayoutInflater) Activity_CheckPW.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View toastView = inflater.inflate(R.layout.toast_message, null);
                TextView backAgain = (TextView) toastView.findViewById(R.id.toast_content);
                backAgain.setText("If you forgot your password, DSW Reminder will be removed. Touch and hold here to continue.");
                toast.setView(toastView);
                toast.show();
            }
        });

        forgot.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Uri packageUri = Uri.parse("package:" + "com.song.securesms");
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
                startActivity(uninstallIntent);
                return true;
            }
        });
	
	}

    // for inflating a menu button in the action bar
    // check R.menu.check_pw for details
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.check_pw, menu);
        return true;
    }

    // display the tutorial screen when this menu button is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()== R.id.check_menu) {
            Intent tutorial = new Intent(Activity_CheckPW.this, Activity_Tutorial_CheckPW.class);
            startActivity(tutorial);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // action for pressing the back hard key on the phone
    // if the back button is pressed again less than 2000 ms after
    // the first one, the super method (back to previous stage) is called
	@Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast toast = new Toast(Activity_CheckPW.this);
        toast.setDuration(Toast.LENGTH_SHORT);

        LayoutInflater inflater = (LayoutInflater) Activity_CheckPW.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

    /*
     * compare string read from a directory with the newly generated md5 value
     *  @param fileName
     *  @param md5content
     */
	public boolean compareMD5(String fileName, String md5content){
		String savedmd5 = "";
        try {
            File file = new File(getApplicationContext().getFilesDir(), fileName);
			FileInputStream in = new FileInputStream(file);
			ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
			savedmd5 = (String) oin.readObject();
			oin.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(md5content.equals(savedmd5)){
			return true;
		}
		else
			return false;
	}

	public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}
}
