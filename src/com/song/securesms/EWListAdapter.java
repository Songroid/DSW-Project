package com.song.securesms;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by song_jin on 6/20/13.
 */
public class EWListAdapter extends ArrayAdapter<EWData>{

    private static final Integer YELLOW = Color.parseColor("#FF9900");
    private static final Integer GREEN = Color.parseColor("#97CE68");

    private final Context context;
    private final List<EWData> ewDataList;
    private final Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();

    public EWListAdapter(Context context, List<EWData> ewDataList) {
        super(context, R.layout.dialog_popup_ew, ewDataList);
        this.context = context;
        this.ewDataList = ewDataList;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View ewView = inflater.inflate(R.layout.ew_listadapter, parent, false);

        TextView question = (TextView) ewView.findViewById(R.id.ewQuestion);
        question.setText(ewDataList.get(position).getQuestion());

        View fuStripeColor = ewView.findViewById(R.id.ewStripeColor);
        TextView fuQuestionColor = (TextView) ewView.findViewById(R.id.ewQuestion);
        Character c = ewDataList.get(position).getType();

        // prevent state change of radio buttons after scrolling
        RadioGroup rg = (RadioGroup) ewView.findViewById(R.id.userSelection);
        Boolean present = map.get(position);
        if (present!=null && present.booleanValue()){
            RadioButton yes = (RadioButton) ewView.findViewById(R.id.radioYes);
            yes.setChecked(true);
        } else if (present!=null && !present.booleanValue()){
            RadioButton no = (RadioButton) ewView.findViewById(R.id.radioNo);
            no.setChecked(true);
        }

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.radioNo:
                        map.put(position, false);
                        ewDataList.get(position).setRbState(0);
                        break;
                    case R.id.radioYes:
                        map.put(position, true);
                        ewDataList.get(position).setRbState(1);
                        break;
                }
            }
        });

        switch (c) {
            case 'E':
                fuStripeColor.setBackgroundColor(GREEN);
                fuQuestionColor.setTextColor(GREEN);
                break;
            case 'F':
                fuStripeColor.setBackgroundColor(YELLOW);
                fuQuestionColor.setTextColor(YELLOW);
                break;
        }

        return ewView;
    }
}
