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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity is for EW message
 * It is a dialog activity with a question in each row
 */
public class Dialog_PopupEWResponse extends Activity {
    public static final String BROADCAST_ACTION = "com.song.securesms.refresh";
    private String msgContent = "", msgAddress = "", msgUniqueID = "";
    private int carrier;
    private Boolean isEW;
    private String[] question;
    private Intent intent;
    private final List<EWData> ewArrayList = new ArrayList<EWData>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_popup_ew);

        // each carrier has a different value for later usage of parsing text messages
        TelephonyManager manager = (TelephonyManager)Dialog_PopupEWResponse.this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getNetworkOperatorName().equals("Verizon Wireless")){
            carrier = 1;
        } else if ((manager.getNetworkOperatorName().equals("AT&T"))){
            carrier = 2;
        } else if (manager.getNetworkOperatorName().equals("T-Mobile")){
            carrier = 4;
        }

        // get the message content and phone number from intent
        try{
            Bundle bundle = this.getIntent().getExtras();
            msgContent = bundle.getString("content");
            Bundle bundle1 = this.getIntent().getExtras();
            msgAddress = bundle1.getString("address");
            Bundle bundle2 = this.getIntent().getExtras();
            msgUniqueID = bundle2.getString("uniqueID");
            Bundle bundle3 = this.getIntent().getExtras();
            isEW = bundle3.getBoolean("isEW");
        } catch (NullPointerException e){
            e.printStackTrace();
            this.finish();
        }

        // format: remove all \n, split the whole line by question mark
        msgContent = msgContent.replaceAll("\\n", "");
        question = msgContent.split("\\?");

        // set each question into formatted variable
        for(int i=0; i<question.length; i++){
            EWData ew = new EWData();
            ew.setQuestion(question[i]+"?");
            // judge if the message is EW, or FU
            if(!isEW){
                ew.setType('F');
            } else {
                ew.setType('E');
            }
            ewArrayList.add(ew);
        }

        // set view with custom adapter
        final ListView ewListView = (ListView) findViewById(R.id.EWList);
        final EWListAdapter ewAdapter = new EWListAdapter(Dialog_PopupEWResponse.this, ewArrayList);
        ewListView.setAdapter(ewAdapter);

        // set the function of 'Send' button
        Button replyYes = (Button) findViewById(R.id.replyEWYes);
        replyYes.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                String sample = "";
                int flag = 0;

                // get value from radio button
                for (int i=0;i<ewArrayList.size(); i++){
                    // create response string
                    if(ewArrayList.get(i).getRbState()==1){
                        sample = sample + (i+1) + "y";
                    } else if(ewArrayList.get(i).getRbState()==0){
                        sample = sample + (i+1) + "n";
                    } else {
                        flag++;
                        break;
                    }
                }

                // if there's unchecked question, notify the user
                if (flag>0) {
                    Toast.makeText(Dialog_PopupEWResponse.this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                    return;
                }

                // use AsyncTask to connect to the Internet
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new GmailNetworkRequest(Dialog_PopupEWResponse.this).execute(sample);
                } else {
                    Toast.makeText(Dialog_PopupEWResponse.this, "Please check your network connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // set the function of 'Cancel' button
        Button replyNO = (Button) findViewById(R.id.replyEWNO);
        replyNO.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Dialog_PopupEWResponse.this.finish();
            }

        });

    }

    // send EW/FU response to the server by email
    public void sendEWResponse(String s){
        String phoneNum =((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        GMailSender sender = new GMailSender(Dialog_PopupEWResponse.this);
        try{
            sender.sendMail("dsw.ews1@gmail.com", phoneNum, s);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private class GmailNetworkRequest extends AsyncTask<String, Void, String> {

        private Context context;
        private ProgressDialog dialog;

        private GmailNetworkRequest(Context context) {
            this.context = context;
            this.dialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Sending response..");
            this.dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try{
                sendEWResponse(strings[0]);
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
                                Dialog_PopupEWResponse.this.getContentResolver().delete(Uri.parse("content://sms/" + _ID), null,
                                        null);
                                cursor1.moveToNext();
                            }
                        }
                        returnValue = "Response sent!";
                    break;
                }
                return returnValue;
            } catch (Exception e){
                e.printStackTrace();
                return "Response Failed, please check your network";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Toast toast = new Toast(Dialog_PopupEWResponse.this);
                toast.setDuration(Toast.LENGTH_SHORT);
                LayoutInflater inflater = (LayoutInflater) Dialog_PopupEWResponse.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.toast_message, null);
                TextView response = (TextView) view.findViewById(R.id.toast_content);
                response.setText(s);
                toast.setView(view);
                toast.show();
            }
            Dialog_PopupEWResponse.this.finish();
            if (s.equals("Response sent!")) {
                // if sent successfully, broadcast intent to main activity
                // to update the UI
                intent = new Intent(BROADCAST_ACTION);
                sendBroadcast(intent);
            }
        }
    }
}