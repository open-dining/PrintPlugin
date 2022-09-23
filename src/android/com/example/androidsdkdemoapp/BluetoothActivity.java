package com.example.androidsdkdemoapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.example.androidsdkdemoapp.adapter.BluetoothListViewAdapter;
import com.example.androidsdkdemoapp.connection.Connection_Bluetooth;

import honeywell.connection.ConnectionBase;
import honeywell.printer.configuration.fp.PrintQualConfigFP;
import honeywell.printer.configuration.fp.PrinterInformationFP;
import honeywell.printer.configuration.fp.PrinterStatFP;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothActvity";
    private ConnectionBase mConn;
    private List<BluetoothDevice> mArrayBluetoothDevices;
    private ListView mListview;
    private BluetoothListViewAdapter mAdapter;
    private TextView mTvConnectionStatus;
    private ImageView ivBack;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgressDialog;
    private boolean mConnectStatus;
    private PrintQualConfigFP mConfigFpl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        mConnectStatus = false;
        mArrayBluetoothDevices = new ArrayList<BluetoothDevice>();
        mAdapter = new BluetoothListViewAdapter(getApplicationContext(), mArrayBluetoothDevices);
        mListview = findViewById(R.id.lv_bluetooth_devices);
        mListview.setAdapter(mAdapter);
        mTvConnectionStatus = findViewById(R.id.tv_connection_status);
        ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothActivity.super.onBackPressed();
            }
        });
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, "Start connecting to bluetooth item in list");
                mTvConnectionStatus.setText("Connecting");
                mProgressDialog = ProgressDialog.show(BluetoothActivity.this, "Please wait", "Connecting printer...", true);
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        connectBluetoothFP(i);
                        mProgressDialog.dismiss();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (!mConnectStatus) {
                                    mTvConnectionStatus.setText("Connect failed");
                                } else {
                                    mTvConnectionStatus.setText("");
                                }
                            }
                        });
                    }
                };
                thread.start();
            }
        });
        discoverBluetoothDevices();

    }

    public void discoverBluetoothDevices() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // check if bluetooth is not enabled, ask user to turn on Bluetooth firstly
        if (!mBluetoothAdapter.isEnabled()) {
            onDialogTurnOnBluetooth();
        }
        // If another discovery is in progress, cancels it before starting the new one.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Finding devices
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (!mArrayBluetoothDevices.contains(device) && device.getName() != null /*&& isAPrinter(device)*/) {
                    mArrayBluetoothDevices.add(device);
                    mAdapter.notifyDataSetChanged();
                }

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void connectBluetoothFP(int i) {
        String productname = "";
        String printheadinfo = "";
        int resolution = 0;
        String serial = "";
        String labelprinted = "";
        String printSpeed = "";
        String darkness = "";
        boolean result = false;
        try {
            mConn = Connection_Bluetooth.createClient(mArrayBluetoothDevices.get(i).getAddress());
            PrinterStatFP stats = new PrinterStatFP(mConn);
            mConfigFpl = new PrintQualConfigFP(mConn);
            PrinterInformationFP printerInfo = new PrinterInformationFP(mConn);
            Log.i(TAG,"start connecting");
            result = mConn.open();
            if (mConn.getIsOpen()) {
                productname = stats.getPrinterStatFromPath("System Information,Product Name");
                resolution = printerInfo.getTphResolution();
                serial = stats.getPrinterStatFromPath("System Information,Printer Serial Number");
                labelprinted = stats.getPrinterStatFromPath("Print Statistics,Labels Printed");
                Log.i(TAG, "stats list: " + productname + " " + resolution + " " + serial + " " + labelprinted);
                printSpeed = mConfigFpl.getPrintSpeed();
                darkness = mConfigFpl.getDarkness();
                Log.i(TAG, "configs list: printSpeed: " + printSpeed + ", darkness: " + darkness);

                //we will try connecting to make sure connection is established with this device or not
                // if connection is established, app will forward to next screen
                mConnectStatus = true;
                mConn.close();
                Log.i(TAG,"stop connecting");
                Intent intent = new Intent(BluetoothActivity.this, TestApiActivity.class);
                intent.putExtra("macaddress", mArrayBluetoothDevices.get(i).getAddress());
                intent.putExtra("productname", productname);
                intent.putExtra("resolution", resolution + "");
                intent.putExtra("serial", serial);
                intent.putExtra("labelprinted", labelprinted);
                intent.putExtra("connection", "bluetooth");
                intent.putExtra("printSpeed", printSpeed);
                intent.putExtra("darkness", darkness);

                startActivity(intent);

            } else {
                mConnectStatus = false;
            }
            mConn = null;
        } catch (Exception e) {
            mConnectStatus = false;
            Log.e(TAG,"Connect exception");
        }
    }

    private void onDialogTurnOnBluetooth() {
        AlertDialog dialog = new AlertDialog.Builder(BluetoothActivity.this).create();
        dialog.setTitle("Bluetooth is not available");
        dialog.setMessage("Please turn on Bluetooth and try again");
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Closes the dialog and terminates the activity.
                        dialog.dismiss();
                        BluetoothActivity.this.finish();
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

}