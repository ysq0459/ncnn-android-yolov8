// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov8ncnn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    private LinearLayout[] navItems;
    private Fragment homeFragment, imageFragment, settingFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 初始化导航项
        navItems = new LinearLayout[]{
                findViewById(R.id.nav_home),
                findViewById(R.id.nav_data),
                findViewById(R.id.nav_setting)
        };

        // 创建Fragment实例
        homeFragment = new HomeFragment();
        imageFragment = new ImageFragment();
        settingFragment = new SettingFragment();

        // 设置初始选中项
        setSelectedTab(0);

        // 默认显示首页
        switchFragment(homeFragment);

        // 设置导航项点击监听
        for (int i = 0; i < navItems.length; i++) {
            final int position = i;
            navItems[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedTab(position);
                    switchToFragment(position);
                }
            });
        }
    }

    private void setSelectedTab(int position) {
        // 重置所有导航项状态
        for (int i = 0; i < navItems.length; i++) {
            navItems[i].setSelected(i == position);
        }
    }

    private void switchToFragment(int position) {
        switch (position) {
            case 0:
                switchFragment(homeFragment);
                break;
            case 1:
                switchFragment(imageFragment);
                break;
            case 2:
                switchFragment(settingFragment);
                break;
        }
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == currentFragment) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 如果Fragment已添加则显示，否则添加
        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.fragment_container, fragment);
        }

        transaction.commit();
        currentFragment = fragment;
    }
}
