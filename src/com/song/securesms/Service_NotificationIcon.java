package com.song.securesms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

/**
 * This service is used for displaying an icon in the notification bar
 * when the app is running in the background.
 */
public class Service_NotificationIcon extends Service {
    private NotificationManager mNM;
    // unique notification id
    private int NOTIFICATION = 1000;

    /*
     * Service lifecycle
     * This is a started service, which is called via onStartCommand
     * and onDestroy. There's no bind on this type of service.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setNotificationIcon();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNM.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setNotificationIcon(){
        // create logout intent
        Intent switchIntent = new Intent(Service_NotificationIcon.this, Activity_CheckPW.class);
        // if the notification is pressed, current notification task is cleared
        switchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent exitIntent = PendingIntent.getActivity(this, 0, switchIntent, 0);
        // action for pressing the notification
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, Activity_Main.class), 0);

        // content in the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_dsw)
                .setContentTitle("DSW Reminder is running")
                .setContentText("Click twice on the \"back\" button to exit.")
                .setOngoing(true)
                .addAction(R.drawable.notification_action_exit, "Logout now", exitIntent);
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder.setContentIntent(intent);
        mNM.notify(NOTIFICATION, mBuilder.build());
    }
}
