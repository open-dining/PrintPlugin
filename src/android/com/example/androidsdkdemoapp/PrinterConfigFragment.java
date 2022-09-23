package com.example.androidsdkdemoapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.androidsdkdemoapp.connection.Connection_Bluetooth;

import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_TCP;
import honeywell.printer.configuration.fp.PrintQualConfigFP;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PrinterConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PrinterConfigFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = "PrinterConfigFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private static final String ARG_PARAM4 = "param4";

    // TODO: Rename and change types of parameters
    private String mAddress;   //IP
    private int mPort;      //Port
    private Button mBtnSave;
    private TextView mTvResult;
    private TextView mTvConnectionType;
    private ConnectionBase mConn;
    private PrintQualConfigFP mConfigFpl;
    private String mResult;
    private ProgressDialog mProgressDialog;
    private Spinner mSpnSpeed;
    private String mPrinterSpeed, mDarkness;
    private EditText mEtDarkness;
    private SwipeRefreshLayout mRefreshLayout;
    private String[] mSpeedValues = {"25", "50", "75", "100", "125", "150", "175", "200"};
    private Thread mThread;


    public PrinterConfigFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PrinterConfigFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PrinterConfigFragment newInstance(String param1, int param2, String param3, String param4) {
        PrinterConfigFragment fragment = new PrinterConfigFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putInt(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3,param3);
        args.putString(ARG_PARAM4,param4);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAddress = getArguments().getString(ARG_PARAM1);
            mPort = getArguments().getInt(ARG_PARAM2);
            mPrinterSpeed = getArguments().getString(ARG_PARAM3);
            mDarkness = getArguments().getString(ARG_PARAM4);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_printer_config, container, false);
        mBtnSave = view.findViewById(R.id.btn_save);
        mTvResult = view.findViewById(R.id.tv_result);
        mTvConnectionType = view.findViewById(R.id.tv_connection_type);
        mSpnSpeed = view.findViewById(R.id.spn_speed);
        mEtDarkness = view.findViewById(R.id.et_darkness);
        mRefreshLayout = view.findViewById(R.id.swipe_container);
        mResult = ("Current Print Speed: " + mPrinterSpeed + "\r\n");
        mResult = mResult + ("Current Darkness Speed: " + mDarkness);
        mTvResult.setText(mResult);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(false);
                mProgressDialog = ProgressDialog.show(getActivity(), "Please wait", "Applying new Configs to Printer...", true);
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getPrinterConfig();
                        mProgressDialog.dismiss();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mTvResult.setText(mResult);
                            }
                        });
                    }
                });
                mThread.start();
            }
        });
        mSpnSpeed.setAdapter(new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, mSpeedValues));
        try {
            Log.i(TAG, "printer address value: " + mAddress + " port: " + mPort);
            //check this fragment is using by bluetooth or network by checking mPort value
            if (mPort == -1) {
                mConn = Connection_Bluetooth.createClient(mAddress);
                mTvConnectionType.setText("CONNECTION BLUETOOTH");
            } else {
                mConn = Connection_TCP.createClient(mAddress, mPort);
                mTvConnectionType.setText("CONNECTION TCP");

            }
            mConfigFpl = new PrintQualConfigFP(mConn);
        } catch (Exception e) {
            Log.e(TAG,"Connect exception");
        }
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTvResult.setText("Result: ");
                mResult = "";
                Log.i(TAG, "on click set PrintConfig for Address: " + mAddress + " port: " + mPort);
                mProgressDialog = ProgressDialog.show(getActivity(), "Please wait", "Applying new Configs to Printer...", true);
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        setPrinterConfig();
                        mProgressDialog.dismiss();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mTvResult.setText(mResult);
                            }
                        });
                    }
                });
                mThread.start();

            }
        });
        return view;
    }

    private String setPrinterConfig() {
        Log.i(TAG, " set Print Speed = " + mSpnSpeed.getSelectedItem().toString() + ",Darkness = "
                + mEtDarkness.getText().toString());
        try {
            if (mConfigFpl.setPrintSpeed(mSpnSpeed.getSelectedItem().toString())) {
                //until now, we cannot get those data
                mResult = mResult + ("New Print Speed: " + mConfigFpl.getPrintSpeed() + "\r\n");
            } else {
                mResult = mResult + ("Set Print Speed error, Old Print Speed" + mConfigFpl.getPrintSpeed() + "\r\n");
            }
            if (mConfigFpl.setDarkness(mEtDarkness.getText().toString())) {
                mResult = mResult + ("New Darkness: " + mConfigFpl.getDarkness() + "\r\n");
            } else {
                mResult = mResult + ("Set Darkness error, Old Darkness" + mConfigFpl.getDarkness());
            }
        } catch (Exception e) {
            Log.e(TAG,"Connect exception");
        }
        return mResult;
    }

    private void getPrinterConfig() {
        Log.i(TAG, "getPrinterConfig");
        mResult = "";
        try {
            mResult = mResult + ("Current Print Speed: " + mConfigFpl.getPrintSpeed() + "\r\n");
            mResult = mResult + ("Current Darkness Speed: " + mConfigFpl.getDarkness());
        } catch (Exception e) {
            Log.e(TAG,"getPrinterConfig threw exception");
            return;
        }
    }


}

