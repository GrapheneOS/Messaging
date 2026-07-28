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
package com.android.messaging.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import com.android.messaging.R;

public class AccessibilityUtil {
    public static boolean isTouchExplorationEnabled(final Context context) {
        final AccessibilityManager accessibilityManager = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager.isTouchExplorationEnabled();
    }

    public static String getVocalizedPhoneNumber(final Resources res, final String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "";
        }
        final StringBuilder vocalizedPhoneNumber = new StringBuilder();
        for (final char c : phoneNumber.toCharArray()) {
            getVocalizedNumber(res, c, vocalizedPhoneNumber);
        }
        return vocalizedPhoneNumber.toString();
    }

    private static void getVocalizedNumber(final Resources res, final char c,
            final StringBuilder builder) {
        switch (c) {
            case '0':
                builder.append(res.getString(R.string.content_description_for_number_zero));
                builder.append(" ");
                return;
            case '1':
                builder.append(res.getString(R.string.content_description_for_number_one));
                builder.append(" ");
                return;
            case '2':
                builder.append(res.getString(R.string.content_description_for_number_two));
                builder.append(" ");
                return;
            case '3':
                builder.append(res.getString(R.string.content_description_for_number_three));
                builder.append(" ");
                return;
            case '4':
                builder.append(res.getString(R.string.content_description_for_number_four));
                builder.append(" ");
                return;
            case '5':
                builder.append(res.getString(R.string.content_description_for_number_five));
                builder.append(" ");
                return;
            case '6':
                builder.append(res.getString(R.string.content_description_for_number_six));
                builder.append(" ");
                return;
            case '7':
                builder.append(res.getString(R.string.content_description_for_number_seven));
                builder.append(" ");
                return;
            case '8':
                builder.append(res.getString(R.string.content_description_for_number_eight));
                builder.append(" ");
                return;
            case '9':
                builder.append(res.getString(R.string.content_description_for_number_nine));
                builder.append(" ");
                return;
            default:
                builder.append(c);
                return;
        }
    }
}
