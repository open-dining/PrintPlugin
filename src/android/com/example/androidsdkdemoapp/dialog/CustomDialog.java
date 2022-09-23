package com.example.androidsdkdemoapp.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.androidsdkdemoapp.R;
import com.example.androidsdkdemoapp.data.PrinterObject;

public class CustomDialog extends DialogFragment implements
        android.view.View.OnClickListener {
    private static final String TAG = "CustomDialog";
    private EditText mPosX, mPosY, mData;
    private Spinner mType;
    private TextView mTvData, mTvCheckObject;
    private Button mButtonCancel, mButtonDone;

    public interface OnInputListener {
        void sendInput(PrinterObject obj);
    }

    public OnInputListener mOnInputListener;

    public static CustomDialog newInstance() {

        Bundle args = new Bundle();

        CustomDialog fragment = new CustomDialog();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_object, container, false);
        mType = view.findViewById(R.id.spinner_print_type);
        mPosX = view.findViewById(R.id.et_xpos);
        mPosY = view.findViewById(R.id.et_ypos);
        mData = view.findViewById(R.id.et_data);
        mTvData = view.findViewById(R.id.tv_data);
        mTvCheckObject = view.findViewById(R.id.tv_check_data);
        mButtonCancel = view.findViewById(R.id.btn_cancel);
        mButtonDone = view.findViewById(R.id.btn_add);
        mButtonCancel.setOnClickListener(this);
        mButtonDone.setOnClickListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.printer_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mType.setAdapter(adapter);
        mType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = adapterView.getItemAtPosition(i).toString();
                if (text.equals("BOX")) {
                    mData.setVisibility(View.GONE);
                    mTvData.setVisibility(View.GONE);
                } else {
                    mData.setVisibility(View.VISIBLE);
                    mTvData.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                dismiss();
                break;
            case R.id.btn_add:
                boolean isError = false;
                String xPos = mPosX.getText().toString();
                String yPos = mPosY.getText().toString();
                String data = mData.getText().toString();

                if(TextUtils.isEmpty(xPos)) {
                    mPosX.setError("x cannot be null");
                    isError = true;
                }
                if(TextUtils.isEmpty(yPos)) {
                    mPosY.setError("y cannot be null");
                    isError = true;
                }
                if(mData.getVisibility() == View.VISIBLE && TextUtils.isEmpty(data)) {
                    mData.setError("data cannot be null");
                }
                if(isError) {
                    return;
                }
                Intent i = new Intent()
                        .putExtra("type", mType.getSelectedItem().toString())
                        .putExtra("x", Integer.parseInt(xPos))
                        .putExtra("y", Integer.parseInt(yPos))
                        .putExtra("data",data);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
                break;
            default:
                break;
        }
        dismiss();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mOnInputListener
                    = (OnInputListener) getActivity();
        } catch (ClassCastException e) {
            Log.e(TAG, "onAttach: ClassCastException: "
                    + e.getMessage());
        }
    }
}
