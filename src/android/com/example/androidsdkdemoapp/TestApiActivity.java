package com.example.androidsdkdemoapp;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import com.example.androidsdkdemoapp.adapter.TabAdaptor;
import com.google.android.material.tabs.TabLayout;

public class TestApiActivity extends AppCompatActivity {
    private static final String TAG = "TestAPIActivity";
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private String mAddress;
    private int mPort =-1;
    private String mConnectionType;
    private ImageView mIvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_api);
        mToolbar = findViewById(R.id.tool_bar);
        //setSupportActionBar(mToolbar);
        mTabLayout = findViewById(R.id.tab_layout);
        mTabLayout.addTab(mTabLayout.newTab().setText("Printer Info"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Printer Config"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Print Demo"));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        mConnectionType = getIntent().getExtras().getString("connection");
        mIvBack = findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // check this activity is called by Network or Bluetooth Option
        if (mConnectionType.equals("network")) {
            mAddress = getIntent().getExtras().getString("ip");
            mPort = Integer.parseInt(getIntent().getExtras().getString("port"));
        } else {
            mAddress = getIntent().getExtras().getString("macaddress");
        }

        String productname = getIntent().getExtras().getString("productname");
        String resolution = getIntent().getExtras().getString("resolution");
        String serial = getIntent().getExtras().getString("serial");
        String labelprinted = getIntent().getExtras().getString("labelprinted");

        String printSpeed = getIntent().getExtras().getString("printSpeed");
        String darkness = getIntent().getExtras().getString("darkness");

        Log.i(TAG, "Received extras " + mAddress + " " + mPort);
        final ViewPager viewPager = findViewById(R.id.view_pager);
        TabAdaptor adaptor = new TabAdaptor(getSupportFragmentManager(), mTabLayout.getTabCount());
        // send data from activity to fragment
        adaptor.setDataForInforFragment(productname, resolution, serial, labelprinted);
        adaptor.setDataForConfigFragment(mAddress, mPort);
        adaptor.setConfigDataForConfigFragment(printSpeed,darkness);
        viewPager.setAdapter(adaptor);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
}