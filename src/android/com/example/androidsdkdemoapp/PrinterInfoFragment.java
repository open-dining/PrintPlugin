package com.example.androidsdkdemoapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PrinterInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PrinterInfoFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = "PrinterInfoFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private static final String ARG_PARAM4 = "param4";


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String mParam3;
    private String mParam4;
    private TextView mTvProductName;
    private TextView mTvSerial;
    private TextView mTvResolution;
    private TextView mTvPrintedLabel;

    public PrinterInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PrinterInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PrinterInfoFragment newInstance(String param1, String param2,String param3,String param4) {
        PrinterInfoFragment fragment = new PrinterInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);
        args.putString(ARG_PARAM4, param4);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            mParam3 = getArguments().getString(ARG_PARAM3);
            mParam4 = getArguments().getString(ARG_PARAM4);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_printer_info, container, false);
        mTvProductName = view.findViewById(R.id.tv_printer_model);
        mTvResolution = view.findViewById(R.id.tv_printer_resolution);
        mTvSerial = view.findViewById(R.id.tv_printer_serial);
        mTvPrintedLabel = view.findViewById(R.id.tv_printed_label);
        mTvProductName.setText(mParam1);
        mTvResolution.setText(mParam2);
        mTvSerial.setText(mParam3);
        mTvPrintedLabel.setText(mParam4);
        return view;
    }
}