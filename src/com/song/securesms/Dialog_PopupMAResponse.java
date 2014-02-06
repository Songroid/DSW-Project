package com.song.securesms;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Dialog_PopupMAResponse extends Activity {
    public static final String BROADCAST_ACTION = "com.song.securesms.refresh";

	private String msgID = "", msgAddress = "", msgUniqueID = "";
    private int carrier;
    private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_popup_ma);

        // different mechanism for each carrier
        TelephonyManager manager = (TelephonyManager)Dialog_PopupMAResponse.this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getNetworkOperatorName().equals("Verizon Wireless")){
            carrier = 1;
        } else if ((manager.getNetworkOperatorName().equals("AT&T"))){
            carrier = 2;
        } else if (manager.getNetworkOperatorName().equals("T-Mobile")){
            carrier = 4;
        }

		// get the message ID and phone number from intent
        try{
            Bundle bundle = this.getIntent().getExtras();
            msgID = bundle.getString("content");
            Bundle bundle1 = this.getIntent().getExtras();
            msgAddress = bundle1.getString("address");
            Bundle bundle2 = this.getIntent().getExtras();
            msgUniqueID = bundle2.getString("uniqueID");
        } catch (NullPointerException e){
            e.printStackTrace();
            this.finish();
        }

        // get the phone number
        final String phoneNum =((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number(); //get phone number

		// set the response content
		TextView responseMsg = (TextView) findViewById(R.id.popupMsg);
		responseMsg.setText("Click Confirm to respond");
		
		// set the function of 'SMS' button
		Button replyOK = (Button) findViewById(R.id.replySMS);
		replyOK.setOnClickListener(new OnClickListener(){
			
			@Override
			public void onClick(View v) {
                // use AsyncTask to connect to the Internet
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new GmailNetworkRequest(Dialog_PopupMAResponse.this).execute(phoneNum);
                } else {
                    Toast.makeText(Dialog_PopupMAResponse.this, "Please check your network connection", Toast.LENGTH_SHORT).show();
                }
			}
		});
		
		// set the function of 'Cancel' button
		Button replyNO = (Button) findViewById(R.id.replyNO);
		replyNO.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Dialog_PopupMAResponse.this.finish();
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.popup_response, menu);
		return true;
	}

    public void sendMAResponse(String address, String phoneNum, String msgID){
        try{
            // use JavaMail API and Gmail authentication to send the public key via email
            GMailSender sender = new GMailSender(Dialog_PopupMAResponse.this);
            sender.sendMail(address, phoneNum, msgID);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private class GmailNetworkRequest extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog;
        private Context context;

        private GmailNetworkRequest(Context context) {
            this.context = context;
            dialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Sending response..");
            this.dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try{
                sendMAResponse("dsw.med@gmail.com", strings[0], msgID);
                String returnValue = "";
                switch (carrier){
                    /** VZW & ATT & TMB **/
                    case 1:
                    case 2:
                    case 4:
                        String[] uniqueID = new String[msgUniqueID.length()/13];
                        int index = 0;
                        for (int i=0; i<msgUniqueID.length()/13; i++){
                            uniqueID[i] = msgUniqueID.substring(index, index+13);
                            index += 13;
                        }
                        Uri uri1 = Uri.parse("content://sms/inbox");
                        // SQLite: where DATE in (?,?,?)
                        String selection = "date"+" in(";
                        for (int i=0; i<uniqueID.length; i++){
                            selection += "?,";
                        }
                        selection = selection.substring(0, selection.length()-1)+")";
                        Cursor cursor1 = getContentResolver().query(uri1, new String[] {"_id","date"},
                                selection, uniqueID, null);
                        if (cursor1.moveToFirst()){
                            for (int i=0; i<cursor1.getCount(); i++){
                                String _ID = cursor1.getString(cursor1.getColumnIndex("_id"));
                                Dialog_PopupMAResponse.this.getContentResolver().delete(Uri.parse("content://sms/" + _ID), null,
                                        null);
                                cursor1.moveToNext();
                            }
                        }
                        returnValue = "Response sent!";
                        break;
                }
                return returnValue;
            } catch (Exception e){
                Log.e("MA Response", e.toString());
                return "Response Failed";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Toast toast = new Toast(Dialog_PopupMAResponse.this);
                toast.setDuration(Toast.LENGTH_SHORT);
                LayoutInflater inflater = (LayoutInflater) Dialog_PopupMAResponse.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.toast_message, null);
                TextView response = (TextView) view.findViewById(R.id.toast_content);
                response.setText(s);
                toast.setView(view);
                toast.show();
            }
            Dialog_PopupMAResponse.this.finish();
            if (s.equals("Response sent!")) {
                intent = new Intent(BROADCAST_ACTION);
                sendBroadcast(intent);
            }
        }
    }
}
