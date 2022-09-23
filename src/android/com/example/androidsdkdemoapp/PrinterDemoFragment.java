package com.example.androidsdkdemoapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.androidsdkdemoapp.adapter.ListViewAdapter;
import com.example.androidsdkdemoapp.connection.Connection_Bluetooth;
import com.example.androidsdkdemoapp.data.PrinterObject;
import com.example.androidsdkdemoapp.dialog.CustomDialog;
import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_TCP;
import honeywell.printer.DocumentFP;
import honeywell.printer.ParametersFP;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PrinterDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PrinterDemoFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PrinterDemoFragment";
    public static final int DIALOG_ADD_OBJECT = 1;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private ListView mLvObject;
    private ListViewAdapter mAdapter;
    private Button mBtnAdd, mBtnPrint, mBtnCancel;
    private List<PrinterObject> mListObj;
    private ConnectionBase mConn;
    private DocumentFP mDocumentFpl;
    private ParametersFP mParams;
    private ProgressDialog mProgressDialog;

    // TODO: Rename and change types of parameters
    private String mAddress;
    private int mPort;

    public PrinterDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PrinterDemoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PrinterDemoFragment newInstance(String param1, int param2) {
        PrinterDemoFragment fragment = new PrinterDemoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putInt(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAddress = getArguments().getString(ARG_PARAM1);    //
            mPort = getArguments().getInt(ARG_PARAM2);       //Port
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_printer_demo, container, false);
        mListObj = new ArrayList<PrinterObject>();
        mListObj.add(new PrinterObject("QRCODE", 1, 1, "123"));
        mLvObject = view.findViewById(R.id.lv_object);
        mAdapter = new ListViewAdapter(getActivity(), mListObj);
        mLvObject.setAdapter(mAdapter);

        mBtnAdd = view.findViewById(R.id.btn_add);
        mBtnPrint = view.findViewById(R.id.btn_print);
        mBtnCancel = view.findViewById(R.id.btn_clear);
        mBtnAdd.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mBtnPrint.setOnClickListener(this);

//        mBtnPrint.setOnClickListener(this);
//        mBtnCancel.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_add:
                CustomDialog dialog = new CustomDialog();
                Bundle args = new Bundle();
                args.putString("pickerStyle", "fancy");
                dialog.setArguments(args);
                dialog.setTargetFragment(this, DIALOG_ADD_OBJECT);
                dialog.show(getFragmentManager(), "CustomDialog");
                break;
            case R.id.btn_print:
                mProgressDialog = ProgressDialog.show(getActivity(), "Please wait", "Printing...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!doPrint()) {
                            mProgressDialog.dismiss();
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    onDialogListEmpty();
                                }
                            });

                        } else {
                            mProgressDialog.dismiss();
                        }
                    }
                }).start();
                break;
            case R.id.btn_clear:
                mListObj.clear();
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;


        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            // add new Print object to ListView
            case DIALOG_ADD_OBJECT:
                if (resultCode == Activity.RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String type = bundle.getString("type");
                    int x = bundle.getInt("x");
                    int y = bundle.getInt("y");
                    String printdata = bundle.getString("data");
                    PrinterObject newobj = new PrinterObject(type, x, y, printdata);
                    mListObj.add(newobj);
                    mAdapter.notifyDataSetChanged();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                }
        }
    }

    public boolean doPrint() {
        if (mListObj.isEmpty()) {
            return false;
        }
        for (int i = 0; i < mListObj.size(); i++) {
            preparePrint(mListObj.get(i).getType(), mListObj.get(i).getX(), mListObj.get(i).getY(), mListObj.get(i).getData());
        }
        startPrint();
        return true;
    }

    public void preparePrint(String type, int x, int y, String data) {
        if (mConn == null) {
            try {
                if (mPort != -1) {
                    Log.i(TAG, "preparePrint calls connect TCP with Address: " + mAddress + " Port: " + mPort);
                    mConn = Connection_TCP.createClient(mAddress, mPort);
                } else {
                    Log.i(TAG, "preparePrint calls connect Bluetooth with Address: " + mAddress);
                    mConn = Connection_Bluetooth.createClient(mAddress);
                }

                if (!mConn.getIsOpen()) {
                    mConn.open();
                }
            } catch (Exception e) {
                Log.e(TAG,"Connect exception");
            }
        }
        if (mParams == null) {
            Log.i(TAG, "preparePrint:create new Parameter for Printing ");
            mParams = new ParametersFP();
        }
        if (mDocumentFpl == null) {
            Log.i(TAG, "preparePrint: create new Document for Printing");
            mDocumentFpl = new DocumentFP();
        }
        switch (type) {
            case "QRCODE":
                Log.i(TAG, "preparePrint:writeBarCode");
                mDocumentFpl.writeBarCode(DocumentFP.BarcodeType.QRCODE, data, x, y, mParams);
                break;
            case "TEXT":
                Log.i(TAG, "preparePrint:writeText");
                mDocumentFpl.writeText(data, x, y, mParams);
                break;
            case "BOX":
                Log.i(TAG, "preparePrint:writeBox");
                mDocumentFpl.writeBox(x, y, 100, 100, 100, data, 5, 1, "|||", "---", mParams);
                break;
        }

    }

    private void startPrint() {
        Log.i(TAG, "startPrint");
        mDocumentFpl.printDocument();
        byte[] documentData = mDocumentFpl.getDocumentData();
        try {
            Log.i(TAG, "begin print");
            mConn.write(documentData);
            Thread.sleep(500);
            Log.i(TAG, "End print");
            mDocumentFpl = null;
        } catch (
                InterruptedException e) {
            Log.e(TAG, "Exception: " + e);
        }

    }

    private void onDialogListEmpty() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setTitle("Empty object");
        dialog.setMessage("Please add at least 1 object to print");
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Closes the dialog and terminates the activity.
                        dialog.dismiss();
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConn != null && mConn.getIsOpen()) mConn.close();

    }
}