package com.tencent.yolov8ncnn;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        TextView textView = view.findViewById(R.id.fragment_text);
        textView.setText("Hg²⁺ colorimetric analysis");

        Button buttonHistoricalData = view.findViewById(R.id.buttonHistoricalData);
        buttonHistoricalData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开历史数据界面
                Intent intent = new Intent(getActivity(), HistoricalDataActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }
}