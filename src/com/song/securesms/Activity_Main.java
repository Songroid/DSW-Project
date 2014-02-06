package com.song.securesms;

import java.io.*;
import java.util.*;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main activity containing a listview for message, a navigation drawer and
 * a menu button for refreshing. Each item in the listview provides further interactions
 */
public class Activity_Main extends Activity {
    static final String EW_FU_MESSAGE_END = "Please click here to answer the questions.";
    static final String KEY_GENERATOR = "dsw.med@gmail.com";
    static final int TTL_HOUR = 60*60*1000;
    static final boolean isTTLFuncOpened = true;

    private boolean doubleBackToExitPressedOnce = false;
    private boolean condition, conditionDelete;
    private byte[] output;
    private int carrier;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout dl;
    private DrawerListAdapter drawerAdapter;
    private HandleKey func;
    private List<SMSData> smsList = new ArrayList<SMSData>();
    private List<DrawerData> drawerList = new ArrayList<DrawerData>();
    private List<VZWData> vzwList = new ArrayList<VZWData>();
    private List<VZWData> vzwMsgList = new ArrayList<VZWData>();
    private ListView smsListView, drawerListView;
    private SMSListAdapter myAdapter;
    private String passphrase, outputStr;

    // register a broadcast receiver for receiving response from window dialog
    // if received, refresh the ui automatically
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // get passphrase from intent (from other activity which switches to main)
        try{
            Bundle extras = getIntent().getExtras();
            passphrase = extras.getString("passkey");
        } catch (NullPointerException e){
            e.printStackTrace();
            this.finish();
        }

        // different mechanism for each carrier
        TelephonyManager manager = (TelephonyManager)Activity_Main.this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getNetworkOperatorName().equals("Verizon Wireless")){
            carrier = 1;
        } else if (manager.getNetworkOperatorName().equals("AT&T")){
            carrier = 2;
        } else if (manager.getNetworkOperatorName().equals("T-Mobile")){
            carrier = 4;
        }
        func = new HandleKey(Activity_Main.this, carrier);

        // check whether the app is first-time launched
        // if it is, generate a new key automatically
        SharedPreferences runCheck = getSharedPreferences("hasRunBefore", 0);
        Boolean hasRun = runCheck.getBoolean("hasRun",false);
        if(!hasRun){
            SharedPreferences settings = getSharedPreferences("hasRunBefore", 0);
            SharedPreferences.Editor edit = settings.edit();
            edit.putBoolean("hasRun", true); //set to has run
            edit.commit();
            generateKey();
        }

        // pull message from inbox
        getMessage();

        // create list view in the main activity
        createMainList();

        // implement drawer list view
        createDrawer();

        // register BroadcastReceiver by passing in an IntentFilter
        registerReceiver(br, new IntentFilter(Dialog_PopupMAResponse.BROADCAST_ACTION));
        registerReceiver(br, new IntentFilter(Dialog_PopupEWResponse.BROADCAST_ACTION));
    }

    // for inflating a menu button in the action bar
    // check R.menu.check_pw for details
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // refresh the list when this menu button is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId()== R.id.main_get_msg) {
            updateUI();
            Toast toast = new Toast(Activity_Main.this);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER|Gravity.TOP,0,80);

            LayoutInflater inflater = (LayoutInflater) Activity_Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.toast_message, null);
            TextView msgRefreshTv = (TextView) view.findViewById(R.id.toast_content);
            msgRefreshTv.setText("Message Refreshed");
            toast.setView(view);
            toast.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // must call onPrepareOptionsMenu() if update the menu status
    // this method is triggered by invalidateOptionsMenu()
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = dl.isDrawerOpen(drawerListView);
        menu.findItem(R.id.main_get_msg).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    // menu key action
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_MENU:
                if (!dl.isDrawerOpen(drawerListView)){
                    dl.openDrawer(drawerListView);
                    return true;
                } else {
                    dl.closeDrawer(drawerListView);
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
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
        Toast toast = new Toast(Activity_Main.this);
        toast.setDuration(Toast.LENGTH_SHORT);

        LayoutInflater inflater = (LayoutInflater) Activity_Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast_message, null);
        TextView backAgain = (TextView) view.findViewById(R.id.toast_content);
        backAgain.setText("Please click BACK again to exit");
        toast.setView(view);
        toast.show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    // if the main activity is back to foreground from completely hidden,
    // hide the icon in the notification bar (stop service)
    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this, Service_NotificationIcon.class);
        stopService(i);
    }

    // if the main activity is back to foreground from completely hidden,
    // or partially hidden, hide the icon in the notification bar (stop service)
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    // if the main activity goes to background (completely hidden),
    // show the icon in the notification bar (start service)
    @Override
    protected void onStop() {
        super.onStop();
        // create service to display icon in the title bar
        Intent i = new Intent(this, Service_NotificationIcon.class);
        startService(i);
    }

    // if the main activity is disposed by the garbage collector,
    // hide the icon in the notification bar (stop service)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(this, Service_NotificationIcon.class);
        stopService(i);
    }

    // adjust message format (Verizon)
    public String handleVZWSMS(String s){
        return s.substring(1);
    }

    // get content from test message (AT&T)
    public String handleATTSMS(String s){
        return s.split("MSG:")[1];
    }

    // get content from test message (T-Mobile)
    public String handleTMBSMS(String s){
        return s.substring(s.indexOf('\n')+1);
    }

	// use paraphrase to encrypt the private key
    public void createAES(RSAPrivateKeySpec priv, String passphrase){
		try {
			// create AES cipher using paraphrase as key
			// MD5 digest has fixed key size 128 bit
			byte[] keyAES = passphrase.getBytes("UTF-8");
			Cipher c = Cipher.getInstance("AES");
			SecretKeySpec keyAESspec = new SecretKeySpec(keyAES, "AES");
			c.init(Cipher.ENCRYPT_MODE, keyAESspec);
			
			// read private key and encrypt it
            File aesFile = new File(Activity_Main.this.getFilesDir(), "aesTransPriv.key");
            ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(aesFile)));
			
			// convert BigInteger modulus and exponent to strings
			String privMod = priv.getModulus().toString();
			String privExp = priv.getPrivateExponent().toString();
			
			// convert modulus and exponent strings to byte arrays and encrypt via AES.
            // convert resultant byte array to a hex string.
            // write the hex string to file.
			oout.writeObject(func.asHex(c.doFinal(privMod.getBytes("UTF-8"))));
			oout.writeObject(func.asHex(c.doFinal(privExp.getBytes("UTF-8"))));
			oout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    // delete specific messages from the message inbox
    public void msgDelete(){
    	// link to SQLite of test message, query this URI and get a return of cursor over result
		Uri uri = Uri.parse("content://sms/inbox");
		Cursor c = getContentResolver().query(uri, new String[] {"_id","thread_id","address","person","date","body","type"}, null, null, null);

        if(c.moveToFirst()){
			for(int i=0;i<c.getCount();i++){
				String threadID = c.getString(c.getColumnIndex("thread_id"));
				String address = c.getString(c.getColumnIndex("address"));
				
				// retrieve messages which are sent from our server
				// then add them to the array
                switch (carrier){
                    /** VZW & ATT & TMB **/
                    case 1:
                    case 2:
                    case 4:
                        conditionDelete = address.equals("dsw.med@gmail.com")||
                                address.equals("dsw.ews1@gmail.com")||
                                address.equals("dsw.note@gmail.com")||
                                address.matches("111\\d{7}")||
                                address.matches("121\\d{7}");
                        break;
                }
                if(conditionDelete){
					String[] factor = new String[]{address};
                    // delete message
                    // same as "SELECT threadID FROM sms/conversations WHERE address = factor"
					Activity_Main.this.getContentResolver().delete(Uri.parse("content://sms/conversations/"+threadID), "address=?", factor);
				}
				c.moveToNext();	
			}
		}
		c.close();
    }

    /*
     * retrieve messages from inbox
     * the goal is to put each message in an arraylist
     * which is the essence of the listview in main UI
     */
    public void getMessage(){
        // get current system time for time update display
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date msgTime = new Date(System.currentTimeMillis());
        TextView msgUpdate = (TextView) findViewById(R.id.msgTimeUpdate);
        msgUpdate.setText("Last update: "+format.format(msgTime));

        // initialize arraylist when re-click
        smsList.clear();
        vzwList.clear();
        vzwMsgList.clear();
        // use arraylist to store retrieved sms
        Uri uri = Uri.parse("content://sms/inbox");
        // columns that we need to select
        Cursor c = getContentResolver().query(uri, new String[] {"_id","thread_id","address","person","date","body","type"}, null, null, null);
        // read the sms data and store it in the list
        if(c.moveToFirst()){
            for(int i=0;i<c.getCount();i++){
                String address = c.getString(c.getColumnIndex("address"));
                String uniqueID = c.getString(c.getColumnIndex("date"));

                // each carrier has different sender address
                switch (carrier){
                    /** VZW & ATT & TMB**/
                    case 1:
                    case 2:
                    case 4:
                        condition = address.equals("dsw.med@gmail.com")||
                                    address.equals("dsw.ews1@gmail.com")||
                                    address.equals("dsw.note@gmail.com")||
                                    address.matches("111\\d{7}")||
                                    address.matches("121\\d{7}");
                        break;
                }

                // only retrieve message from numbers or email addresses of our server
                if(condition){
                    SMSData sms = new SMSData();
                    // all carrier's format is based on the one of Verizon
                    // this variable contains all attributed needed for a single text message
                    VZWData vzwData = new VZWData();
                    sms.setNumber(address);
                    vzwData.setNumber(address);
                    // convert to date format
                    Date date = new Date(c.getLong(c.getColumnIndexOrThrow("date")));
                    String formattedDate = new SimpleDateFormat("MM/dd HH:mm:ss").format(date);
                    char[] hexArray = new char[]{};

                    // decrypt by RSA algorithm
                    try {
                        switch (carrier){
                            /** VZW **/
                            case 1:
                                hexArray = handleVZWSMS(c.getString(c.getColumnIndexOrThrow("body")).toString()).toCharArray();
                                break;
                            /** ATT **/
                            case 2:
                                hexArray = handleATTSMS(c.getString(c.getColumnIndexOrThrow("body")).toString()).toCharArray();
                                break;
                            /** TMB **/
                            case 4:
                                hexArray = handleTMBSMS(c.getString(c.getColumnIndexOrThrow("body")).toString()).toCharArray();
                                break;
                        }
                        output = func.rsaDecrypt(Hex.decodeHex(hexArray), passphrase, 1);
                        outputStr = new String(output,"UTF-8");
                        // split to retrieve corresponding block of message
                        // e.g. 1|3|1MA|1|Please take **** 2|3|3FU|2.Do you have problem using this";
                        String numID = outputStr.split("\\|")[2];
                        String messageID = new Scanner(numID).findInLine("\\d+");
                        // set each attribute value based on block content
                        if (messageID != null){
                            vzwData.setIndex(Integer.parseInt(outputStr.split("\\|")[0]));
                            vzwData.setBlocks(Integer.parseInt(outputStr.split("\\|")[1]));
                            vzwData.setId(Integer.parseInt(messageID));
                            vzwData.setType(numID.replaceAll(messageID,"").charAt(0));
                            vzwData.setUniqueID(uniqueID);
                            vzwData.setTime(formattedDate);
                            // only for MA message
                            if (vzwData.getType()=='M'){
                                vzwData.setContent(outputStr.split("\\|")[4]);
                                vzwData.setTtl(Integer.parseInt(outputStr.split("\\|")[3]));
                                // delete the message if TTL expires
                                if (msgTime.getTime() - Long.valueOf(vzwData.getUniqueID())>TTL_HOUR*vzwData.getTtl()){
                                    vzwData.setShouldDelete(true);
                                }
                            // for EW and FU message, there's no TTL part
                            // so get the content directly
                            } else {
                                vzwData.setContent(outputStr.split("\\|")[3]);
                            }
                        }
                        vzwList.add(vzwData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                c.moveToNext();
            }
        }
        c.close();

        if (isTTLFuncOpened) {
            // delete the message if time-out
            Iterator<VZWData> itr = vzwList.iterator();
            while (itr.hasNext()) {
                VZWData v = itr.next();
                if (v.isShouldDelete()) {
                    itr.remove();
                    // delete the sms in the inbox
                    Uri uri2 = Uri.parse("content://sms/inbox");
                    Cursor cursor2 = getContentResolver().query(uri2, new String[] {"_id","date"},
                            "date in (?)", new String[]{v.getUniqueID()}, null);
                    if (cursor2.moveToFirst()){
                        for (int j=0; j<cursor2.getCount(); j++){
                            String _ID = cursor2.getString(cursor2.getColumnIndex("_id"));
                            Activity_Main.this.getContentResolver().delete(Uri.parse("content://sms/" + _ID), null,
                                    null);
                            cursor2.moveToNext();
                        }
                    }
                }
            }
        }

        // concatenate VZW message
        /** VZW & ATT **/
        if ((carrier==1)||(carrier==2)||(carrier==4)) {
            // HashMap: [id, index] -> [VZWData]
            // use key in HashMap to enable grouping
            VZWKey gKey;
            HashMap<VZWKey, VZWData> vzwHash = new HashMap<VZWKey, VZWData>();

            // use hashmap to prevent from duplicate message (unique id and index)
            for (VZWData v : vzwList) {
                gKey = new VZWKey(v.getId(), v.getIndex());
                vzwHash.put(gKey, v);
            }

            // sort HashMap into ArrayList
            List<VZWData> sortedVZW = new ArrayList<VZWData>(vzwHash.values());

            // sort by type -> id -> index
            Collections.sort(sortedVZW, new Comparator<VZWData>() {
                @Override
                public int compare(VZWData obj1, VZWData obj2) {
                    int typeSub = obj1.getType() - obj2.getType();
                    if (typeSub==0){
                        int idSub = obj1.getId() - obj2.getId();
                        if (idSub==0){
                            return obj1.getIndex() - obj2.getIndex();
                        }
                        return idSub;
                    }
                    return typeSub;
                }
            });

            // concatenate all separated message
            VZWData finalMsg = new VZWData();
            String content = "";
            // check if there's missing message
            StringBuilder smsUniqueIDGroup = new StringBuilder();
            // flag for incomplete message
            Boolean incmpFlag = false;
            int count = 0, tempID = 0, tempBlocks = 0, tempCount = 0, tempIndex = 0;
            char tempType = '\u0000'; // initial value for char

            for (VZWData v : sortedVZW){
                // concatenate each block together
                // when there's a new id and type in the list,
                // check previous message using temp blocks from storage
                // since the list has already been sorted, the count of sub-message
                // in each type should be the same with the number of index
                if((tempID!=v.getId())||(tempType!=v.getType())){
                    // if there's an incomplete message, flush the whole incomplete message in the list
                    if (tempCount!=tempBlocks){
                        incmpFlag = true;
                        finalMsg = new VZWData();
                        smsUniqueIDGroup = new StringBuilder();
                        content = "";
                        count=0;
                        // if isolated index=block message is received, remove it from UI list
                        if (tempIndex==tempBlocks) {
                            vzwMsgList.remove(vzwMsgList.size()-1);
                        }
                    }
                }
                // if the first block of message is lost,
                // the remaining blocks won't be concatenated
                if(v.getIndex()==1) {
                    finalMsg.setType(v.getType());
                    finalMsg.setId(v.getId());
                    finalMsg.setTime(v.getTime());
                    finalMsg.setNumber(v.getNumber());
                    if (v.getType()=='M'){
                        finalMsg.setTtl(v.getTtl());
                    }
                }
                // store the current attributes for future comparison
                tempID = v.getId();
                tempType = v.getType();
                tempBlocks = v.getBlocks();
                tempIndex = v.getIndex();
                smsUniqueIDGroup.append(v.getUniqueID());
                content += v.getContent();
                count++;
                tempCount = count;
                // e.g. if f33 is received, add whole content to the main list
                if((v.getBlocks()==v.getIndex())) {
                    finalMsg.setContent(content);
                    finalMsg.setSmsUniqueIDGroup(smsUniqueIDGroup.toString());
                    vzwMsgList.add(finalMsg);
                    finalMsg = new VZWData();
                    smsUniqueIDGroup = new StringBuilder();
                    content = "";
                    count=0;
                }
            }
            // notify the user if there's incomplete message
            if(incmpFlag) {
                Toast toast = new Toast(Activity_Main.this);
                toast.setDuration(Toast.LENGTH_SHORT);
                LayoutInflater inflater = (LayoutInflater) Activity_Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.toast_message, null);
                TextView tvNotComplete = (TextView) view.findViewById(R.id.toast_content);
                tvNotComplete.setText("Message not complete");
                toast.setView(view);
                toast.show();
            }

            // transform to proper SMS data
            // coordinate with UI components
            for (VZWData v : vzwMsgList) {
                SMSData sms = new SMSData();
                sms.setNumber(v.getNumber());
                sms.setID(Integer.toString(v.getId()));
                sms.setTime(v.getTime());
                sms.setType(v.getType());
                sms.setSmsUniqueIDGroup(v.getSmsUniqueIDGroup());
                if (v.getType()=='M'){
                    sms.setSender("DSW Medication Reminder");
                    sms.setBody(v.getContent());
                } else if (v.getType()=='E'){
                    sms.setBody(func.addSpaceInEWorFU(v.getContent()));
                    sms.setSender("DSW EWS Survey");
                } else if (v.getType()=='N'){
                    sms.setBody(v.getContent());
                    sms.setSender("DSW Notification");
                } else if (v.getType()=='F'){
                    sms.setBody(func.addSpaceInEWorFU(v.getContent()));
                    sms.setSender("DSW Follow Up");
                }
                smsList.add(sms);
            }

            // sort again by time (whole message)
            Collections.sort(smsList, new Comparator<SMSData>() {
                @Override
                public int compare(SMSData smsData, SMSData smsData2) {
                    return smsData2.getTime().compareTo(smsData.getTime());
                }
            });
        }
    }

    // send key via email
    private void generateKey(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // if a new key is generated, delete all privious ones
            // previous DSW message cannot be decrypted using new keys
            msgDelete();
            new GmailNetworkRequest(Activity_Main.this).execute();
        } else {
            Toast.makeText(Activity_Main.this, "Please check your network connection.", Toast.LENGTH_SHORT).show();
        }
    }

    // use custom adapter to put list content into UI components
    private void createMainList(){
        // configure custom adapter in the listview
        smsListView = (ListView) findViewById(R.id.SMSList);
        myAdapter = new SMSListAdapter(Activity_Main.this, smsList);
        // if the listview is empty, use another view
        smsListView.setEmptyView(findViewById(R.id.empty_list_view));
        smsListView.setAdapter(myAdapter);

        // click message item body to reply
        smsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View view,
                                    int position, long id) {
                // get the message id from custom adapter
                int index = a.getFirstVisiblePosition();
                // use relative positioning to get right row from listview
                View myView = a.getChildAt(position-index);
                // get content from adapter via UI components
                TextView contentComp = (TextView) myView.findViewById(R.id.smsBodyText);
                TextView msgIDComp = (TextView) myView.findViewById(R.id.smsID);
                TextView addressComp = (TextView) myView.findViewById(R.id.smsNumber);
                TextView msgTypeComp = (TextView) myView.findViewById(R.id.smsType);
                TextView msgUniqueIDComp = (TextView) myView.findViewById(R.id.smsUniqueID);
                String content = contentComp.getText().toString();
                String msgID = msgIDComp.getText().toString();
                String msgType = msgTypeComp.getText().toString();
                String msgAddress = addressComp.getText().toString();
                String msgUniqueID = msgUniqueIDComp.getText().toString();

                // add message ID to dialog activity
                // set intent content to other activities
                // N for notification
                if(msgType.charAt(0)=='N'){
                // M for MA message
                } else if(msgType.charAt(0)=='M'){
                    msgID = "(*"+msgID+"*)";
                    Intent intent = new Intent(Activity_Main.this, Dialog_PopupMAResponse.class);
                    intent.putExtra("content", msgID);
                    intent.putExtra("address", msgAddress);
                    intent.putExtra("uniqueID", msgUniqueID);
                    startActivity(intent);
                // E for EW message
                } else if (msgType.charAt(0)=='E'){
                    Intent intent = new Intent(Activity_Main.this, Dialog_PopupEWResponse.class);
                    intent.putExtra("isEW", true);
                    intent.putExtra("content", content.split(EW_FU_MESSAGE_END)[0]);
                    intent.putExtra("address", msgAddress);
                    intent.putExtra("uniqueID", msgUniqueID);
                    startActivity(intent);
                // F for FU message
                } else if (msgType.charAt(0)=='F'){
                    Intent intent = new Intent(Activity_Main.this, Dialog_PopupEWResponse.class);
                    intent.putExtra("isEW", false);
                    intent.putExtra("content", content.split(EW_FU_MESSAGE_END)[0]);
                    intent.putExtra("address", msgAddress);
                    intent.putExtra("uniqueID", msgUniqueID);
                    startActivity(intent);
                }
            }
        });

        // long click to delete
        smsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View view, final int position, long l) {
                int index = a.getFirstVisiblePosition();
                View itemView = a.getChildAt(position - index);

                // display an image if long click
                final ImageView selectTick = (ImageView) itemView.findViewById(R.id.selectTick);
                selectTick.setVisibility(0);

                // alert dialog
                AlertDialog.Builder builder = new Builder(Activity_Main.this);
                builder.setMessage("Are you sure you want to delete this message?");
                builder.setNegativeButton("No",
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                selectTick.setVisibility(View.GONE);
                                dialogInterface.dismiss();
                            }
                        });
                builder.setPositiveButton("Yes",
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int integer) {
                                switch (carrier){
                                    /** VZW & ATT & TMB **/
                                    case 1:
                                    case 2:
                                    case 4:
                                        // unique ID is composed of blocks of time number, which is 13-digit long
                                        // since most UI row is a combination of several messages, if we need to
                                        // delete individual message in the message inbox, it's better to use
                                        // time as a unique ID of the message
                                        String msgUniqueID = myAdapter.getItem(position).getSmsUniqueIDGroup();
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
                                                Activity_Main.this.getContentResolver().delete(Uri.parse("content://sms/" + _ID), null,
                                                        null);
                                                cursor1.moveToNext();
                                            }
                                        }
                                        break;
                                }
                                // Notify PullToRefreshAttacher that the refresh has finished
                                updateUI();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

                Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (negative != null) {
                    negative.setBackgroundResource(R.drawable.btn_holo_light);
                    negative.setTextColor(Color.WHITE);
                }
                return true;
            }
        });

        // create a PullToRefreshAttacher instance
        // override methods: onRefreshStarted(View view)
    }

    // create navigation drawer
    private void createDrawer(){
        dl = (DrawerLayout) findViewById(R.id.drawerlayout);
        dl.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // first drawer row
        DrawerData reserved = new DrawerData(); // fourth option for future reserved
        reserved.setContent("Tutorial");
        reserved.setImage('t');
        drawerList.add(reserved);

        // second drawer row
        DrawerData app_about = new DrawerData(); // third option: about the app
        app_about.setContent("About DSW");
        app_about.setImage('a');
        drawerList.add(app_about);

        drawerListView = (ListView) findViewById(R.id.drawer);
        // footer
        View headerView = getLayoutInflater().inflate(R.layout.drawer_header, drawerListView, false);
        headerView.setOnClickListener(null);
        drawerListView.addHeaderView(headerView);
        drawerListView.setBackgroundColor(getResources().getColor(R.color.DRAWER_DAY_COLOR));
        drawerAdapter = new DrawerListAdapter(Activity_Main.this, drawerList);
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View view, int position, long l) {
                int index = a.getFirstVisiblePosition();
                switch (position-index){
                    case 1:
                        Intent tutorial = new Intent(Activity_Main.this, Activity_Tutorial.class);
                        startActivity(tutorial);
                        break;
                    case 2:
                        Intent intent = new Intent(Activity_Main.this, Dialog_AboutApp.class);
                        startActivity(intent);
                        break;
                }
                dl.closeDrawer(drawerListView);
            }
        });
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if(android.os.Build.VERSION.SDK_INT>=14) {
            getActionBar().setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = new ActionBarDrawerToggle(this, dl, R.drawable.ic_navigation_drawer, R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerClosed(View drawerView) {
                invalidateOptionsMenu();
                getActionBar().setTitle(R.string.app_name);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
                getActionBar().setTitle(R.string.drawer_title_open);
            }
        };
        dl.setDrawerListener(drawerToggle);
    }

    // refresh the list view
    private void updateUI() {
        getMessage();
        myAdapter.notifyDataSetChanged();
    }

    // use asynchronous task to send email in the background
    private class GmailNetworkRequest extends AsyncTask<Void, Void, String>{
        private ProgressDialog dialog;
        private Context context;

        public GmailNetworkRequest(Activity_Main activity){
            context = activity;
            dialog = new ProgressDialog(context);
        }

        // present other interactions during message sending
        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Sending request..");
            this.dialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            // use JavaMail API and Gmail authentication to send the public key via email
            String phoneNum =((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number(); //get phone number
            GMailSender sender = new GMailSender(Activity_Main.this);
            try {
                HashMap<String,KeySpec> map = func.rsaKeyGen();
                // store AES encryption
                createAES((RSAPrivateKeySpec) map.get("private"), passphrase);
                sender.sendMailWithPubKey(KEY_GENERATOR, phoneNum, (RSAPublicKeySpec) map.get("public"));
                return "A new key is generated";
            } catch (android.content.ActivityNotFoundException e){
                e.printStackTrace();
                return "There are no email clients installed";
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(Activity_Main.this, result, Toast.LENGTH_SHORT).show();
        }
    }

}
