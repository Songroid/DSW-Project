package com.song.securesms;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by song_jin on 6/20/13.
 */
public class SMSListAdapter extends ArrayAdapter<SMSData>{

    private static final Integer YELLOW = Color.parseColor("#FF9900");
    private static final Integer GREEN = Color.parseColor("#97CE68");
    private static final Integer RED = Color.parseColor("#FF6766");
    private static final Integer BLUE = Color.parseColor("#6BCBCA");


    private final Context context;
	private final List<SMSData> smsList;

	public SMSListAdapter(Context context, List<SMSData> smsList) {
		super(context, R.layout.activity_main, smsList);
		this.context = context;
		this.smsList = smsList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View rowView = inflater.inflate(R.layout.sms_listadapter, parent, false);

		TextView senderNumber = (TextView) rowView.findViewById(R.id.smsNumberText);
		senderNumber.setText(smsList.get(position).getSender());

        TextView receiveTime = (TextView) rowView.findViewById(R.id.smsTimeText);
        receiveTime.setText(smsList.get(position).getTime());

		TextView msgBody = (TextView) rowView.findViewById(R.id.smsBodyText);
		msgBody.setText(smsList.get(position).getBody());

        TextView msgID = (TextView) rowView.findViewById(R.id.smsID);
        msgID.setText(smsList.get(position).getID());

        TextView msgType = (TextView) rowView.findViewById(R.id.smsType);
        msgType.setText(smsList.get(position).getType().toString());

        TextView msgAddress = (TextView) rowView.findViewById(R.id.smsNumber);
        msgAddress.setText(smsList.get(position).getNumber());

        TextView msgUniqueID = (TextView) rowView.findViewById(R.id.smsUniqueID);
        msgUniqueID.setText(smsList.get(position).getSmsUniqueIDGroup());

        TextView smsIndication = (TextView) rowView.findViewById(R.id.smsIndication);

        Character c = smsList.get(position).getType();
        View stripeColor = rowView.findViewById(R.id.smsColor);
        TextView numberTextColor = (TextView) rowView.findViewById(R.id.smsNumberText);
        TextView timeTextColor = (TextView) rowView.findViewById(R.id.smsTimeText);

        // set different color stripe based on the type of message
        switch (c) {
            case 'M':
                stripeColor.setBackgroundColor(RED);
                numberTextColor.setTextColor(RED);
                timeTextColor.setTextColor(RED);
                smsIndication.setTextColor(RED);
                break;
            case 'E':
                stripeColor.setBackgroundColor(GREEN);
                numberTextColor.setTextColor(GREEN);
                timeTextColor.setTextColor(GREEN);
                smsIndication.setTextColor(GREEN);
                break;
            case 'N':
                stripeColor.setBackgroundColor(BLUE);
                numberTextColor.setTextColor(BLUE);
                timeTextColor.setTextColor(BLUE);
                smsIndication.setVisibility(8);
                break;
            case 'F':
                stripeColor.setBackgroundColor(YELLOW);
                numberTextColor.setTextColor(YELLOW);
                timeTextColor.setTextColor(YELLOW);
                smsIndication.setTextColor(YELLOW);
                break;
        }
		
		return rowView;
	}
}
