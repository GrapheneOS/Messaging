/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui;

import static com.android.messaging.util.BuglePrefs.KEY_LAST_VERSION;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.android.messaging.BuildConfig;
import com.android.messaging.R;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.UiUtils;

/**
 * Activity to warn user that sms/mms is insecure and should not be used.
 */
public class WelcomeSMSWarningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome_sms_warning);
        UiUtils.setStatusBarColor(this, getColor(R.color.permission_check_activity_background));

        // Remove title bar added by AppCompatActivity
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        findViewById(R.id.exit).setOnClickListener(view -> finish());

        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();

        TextView mNextView = findViewById(R.id.next);
        mNextView.setVisibility(View.VISIBLE);

        mNextView.setOnClickListener(v -> {
            UIIntents.get().launchPermissionCheckActivity(this);
            prefs.putInt(KEY_LAST_VERSION, BuildConfig.VERSION_CODE);
            finish();
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
