package com.example.tsensors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/* Main adapter for the list view
* Contains checkbox, textview(fileName), textview(time like ... min ago)*/
public class ListAdapter extends ArrayAdapter<ListItem> {
    Context context;                                                // Main Context
    ArrayList<ListItem> data = new ArrayList<ListItem>();                   // main textview data
    TimeStampFormatter formatter = new TimeStampFormatter();        // formatter for the time textview
    public boolean isCheckboxVisible = false;
    /* Constructor */
    public ListAdapter(Context context, ArrayList<ListItem> dataItem) {
        super(context, R.layout.list_item, dataItem);
        this.data = dataItem;
        this.context = context;
    }

    /* parse Date from the fileName */
    protected long parseDate(String fileName) {
        String temp = fileName.substring(fileName.indexOf(utils.TEMPPATH) + utils.TEMPPATH.length());
        return Long.parseLong(temp);
    }

    /* parse Title from the fileName */
    protected String parseTitle(String fileName) {
        return fileName.substring(0, fileName.indexOf(utils.TEMPPATH));
    }

    /* Contains the all items */
    public class ViewHolder {
        TextView text;
        CheckBox checkBox;
        TextView time;
    }

    /* Get the current item view */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView
                    .findViewById(R.id.childTextView);
            viewHolder.checkBox = (CheckBox) convertView
                    .findViewById(R.id.checkBox);
            viewHolder.time = (TextView) convertView.findViewById(R.id.textTime);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if(isCheckboxVisible)
            viewHolder.checkBox.setVisibility(View.VISIBLE);
        else
            viewHolder.checkBox.setVisibility(View.GONE);
        final String temp = getItem(position).getItem();
        try {
            viewHolder.text.setText(parseTitle(temp));
        } catch(Exception e) {
            e.printStackTrace();
        }
        try {
            viewHolder.time.setText(formatter.format(parseDate(temp)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        viewHolder.checkBox.setTag(position);
        viewHolder.checkBox.setChecked(data.get(position).getSelect());
        viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (checkBoxClickListener != null) {
                    checkBoxClickListener.onCheckBoxClickListener((Integer) v.getTag(), temp);
                }

            }
        });
        return convertView;
    }

    CheckBoxClickListener checkBoxClickListener;

    public interface CheckBoxClickListener {
        public void onCheckBoxClickListener(int position, String value);
    }

    public void setCheckBoxClickListener(CheckBoxClickListener listener) {
        this.checkBoxClickListener = listener;
    }
}
