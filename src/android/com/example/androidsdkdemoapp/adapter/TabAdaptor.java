package com.example.androidsdkdemoapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.example.androidsdkdemoapp.PrinterConfigFragment;
import com.example.androidsdkdemoapp.PrinterDemoFragment;
import com.example.androidsdkdemoapp.PrinterInfoFragment;

public class TabAdaptor extends FragmentStatePagerAdapter {
    private String mProductname;
    private String mResolution;
    private String mSerial;
    private String mPrintedLabel;

    private String mIp;
    private int mPort;

    private String mPrintSpeed;
    private String mDarkness;
    public TabAdaptor(@NonNull FragmentManager fm, int behavior) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }
    public void setDataForInforFragment(String productname, String resolution, String serial, String printedlabel) {
        mProductname = productname;
        mResolution = resolution;
        mSerial = serial;
        mPrintedLabel = printedlabel;
    }
    public void setDataForConfigFragment(String ip, int port) {
        mIp = ip;
        mPort = port;
    }
    public void setConfigDataForConfigFragment(String speed, String darkness) {
        mPrintSpeed = speed;
        mDarkness = darkness;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment= PrinterInfoFragment.newInstance(mProductname,mResolution,mSerial,mPrintedLabel);
                break;
            case 1:
                fragment = PrinterConfigFragment.newInstance(mIp,mPort, mPrintSpeed, mDarkness);
                break;
            case 2:
                fragment = PrinterDemoFragment.newInstance(mIp,mPort);
                break;
            default:
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
