package com.example.androidsdkdemoapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_TCP;
import honeywell.printer.configuration.fp.PrintQualConfigFP;
import honeywell.printer.configuration.fp.PrinterInformationFP;
import honeywell.printer.configuration.fp.PrinterStatFP;

public class NetworkActivity extends AppCompatActivity {
    private static final String TAG = "NetworkActivity";
    private Button mBtnConnect;
    private EditText mPrinterIp;
    private EditText mPrinterPort;
    private ConnectionBase mConn;
    private PrinterStatFP mPrinterStat;
    private PrinterInformationFP mPrinterInformationFpl;
    private TextView mTvResult;
    private ImageView mIvBack;
    private WifiManager mWifiManager;
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private boolean mConnectStatus;
    private PrintQualConfigFP mConfigFpl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        mConnectStatus = false;
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mBtnConnect = findViewById(R.id.btn_connect);
        mPrinterIp = findViewById(R.id.et_printer_ip);
        mPrinterPort = findViewById(R.id.et_printer_port);
        mTvResult = findViewById(R.id.tv_result);
        mIvBack = findViewById(R.id.iv_back);
        //check if wifi is enabled or not
        if (!mWifiManager.isWifiEnabled()) {
            onDialogTurnOnWifi();
        }

        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetworkActivity.super.onBackPressed();
            }
        });
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, mPrinterIp.getText().toString());
                mTvResult.setText("Connecting");
                mProgressDialog = ProgressDialog.show(NetworkActivity.this, "Please wait", "Connecting printer...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        onConnectPrinter();
                        mProgressDialog.dismiss();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (!mConnectStatus) {
                                    mTvResult.setText("Connect failed");
                                } else {
                                    mTvResult.setText("");
                                }
                            }
                        });
                    }
                }).start();

            }

        });
    }

    private void onDialogTurnOnWifi() {
        AlertDialog dialog = new AlertDialog.Builder(NetworkActivity.this).create();
        dialog.setTitle("Wifi is not enabled");
        dialog.setMessage("Please turn on Wifi and try again");
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Closes the dialog and terminates the activity.
                        dialog.dismiss();
                        NetworkActivity.this.finish();
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void onConnectPrinter() {
        String productname = "";
        String printheadinfo = "";
        int resolution = 0;
        String serial = "";
        String labelprinted = "";
        String printSpeed = "";
        String darkness = "";
        boolean result = false;
        try {
            mConn = Connection_TCP.createClient(mPrinterIp.getText().toString(), Integer.parseInt(mPrinterPort.getText().toString()));
            mPrinterStat = new PrinterStatFP(mConn);
            mPrinterInformationFpl = new PrinterInformationFP(mConn);
            mConfigFpl = new PrintQualConfigFP(mConn);
            Log.i(TAG, "start open connection");
            result = mConn.open();
            if (result) {
                productname = mPrinterStat.getPrinterStatFromPath("System Information,Product Name");
                Log.i(TAG, "product name: " + productname);
                resolution = mPrinterInformationFpl.getTphResolution();
                Log.i(TAG, "resolution: " + resolution);
                serial = mPrinterStat.getPrinterStatFromPath("System Information,Printer Serial Number");
                Log.i(TAG, "serial number: " + serial);
                labelprinted = mPrinterStat.getPrinterStatFromPath("Print Statistics,Labels Printed");
                Log.i(TAG, "label printed: " + labelprinted);
                printSpeed = mConfigFpl.getPrintSpeed();
                darkness = mConfigFpl.getDarkness();
                Log.i(TAG, "Printspeed: " + labelprinted + ", Darkness" + darkness);
                mConnectStatus = true;
            }
        } catch (Exception e) {
            mConnectStatus = false;
            Log.e(TAG,"Connect exception");
        }

        if (mConnectStatus) {
            Log.i(TAG, "start close connection");
            mConn.close();
            Log.i(TAG, "stop close connection");
            Intent intent = new Intent(NetworkActivity.this, TestApiActivity.class);
            intent.putExtra("ip", mPrinterIp.getText().toString());
            intent.putExtra("port", mPrinterPort.getText().toString());
            intent.putExtra("productname", productname);
            intent.putExtra("resolution", String.valueOf(resolution));
            intent.putExtra("serial", serial);
            intent.putExtra("labelprinted", labelprinted);
            intent.putExtra("connection", "network");
            intent.putExtra("printSpeed", printSpeed);
            intent.putExtra("darkness", darkness);
            startActivity(intent);
        }

    }
}