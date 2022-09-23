package com.example.androidsdkdemoapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.androidsdkdemoapp.R;
import com.example.androidsdkdemoapp.data.PrinterObject;
import com.example.androidsdkdemoapp.dialog.CustomDialog;

import java.util.List;

public class ListViewAdapter extends BaseAdapter {
    private List<PrinterObject> mListObject;
    private LayoutInflater mInflater;
    private Context context;


        public ListViewAdapter(Context c, List<PrinterObject> list) {
            context =c;
            mListObject = list;
    }
    @Override
    public int getCount() {
        return mListObject.size();
    }

    @Override
    public Object getItem(int i) {
        return mListObject.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            mInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.list_child_object, null);
            TextView type = convertView.findViewById(R.id.tv_type);
            TextView posx = convertView.findViewById(R.id.tv_posx);
            TextView posy = convertView.findViewById(R.id.tv_posy);
            TextView data = convertView.findViewById(R.id.tv_data);
            type.setText(mListObject.get(i).getType());
            posx.setText(Integer.toString(mListObject.get(i).getX()));
            posy.setText(Integer.toString(mListObject.get(i).getY()));
            data.setText(mListObject.get(i).getData());
        }
        return convertView;
    }
}