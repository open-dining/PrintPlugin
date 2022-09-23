package com.example.androidsdkdemoapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.androidsdkdemoapp.R;

import java.util.List;

public class BluetoothListViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<BluetoothDevice> mListPrinter;
    private LayoutInflater mInflater;

    public BluetoothListViewAdapter(Context context, List<BluetoothDevice> listPrinter) {
        this.mContext = context;
        this.mListPrinter = listPrinter;
    }

    @Override
    public int getCount() {
        return mListPrinter.size();
    }

    @Override
    public Object getItem(int i) {
        return mListPrinter.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            mInflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.listview_bluetooth_devices, null);
            TextView name = convertView.findViewById(R.id.tv_bluetooth_device_name);
            TextView address = convertView.findViewById(R.id.tv_bluetooth_address);
            name.setText(mListPrinter.get(i).getName());
            address.setText(mListPrinter.get(i).getAddress());
        }
        return convertView;
    }
}
