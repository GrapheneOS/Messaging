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

package com.android.messaging.ui.appsettings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.BidiFormatter;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.messaging.R;
import com.android.messaging.util.PhoneUtils;

/**
 * Preference that displays a phone number and allows editing via a dialog.
 * <p>
 * A default number can be assigned, which is shown in the preference view and
 * used to populate the dialog editor when the preference value is not set. If
 * the user sets the preference to a number equivalent to the default, the
 * underlying preference is cleared.
 */
public class PhoneNumberPreference extends EditTextPreference {

    private String mDefaultPhoneNumber;
    private int mSubId;

    public PhoneNumberPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mDefaultPhoneNumber = "";
    }

    public void setDefaultPhoneNumber(final String phoneNumber, final int subscriptionId) {
        mDefaultPhoneNumber = phoneNumber;
        mSubId = subscriptionId;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        setOnBindEditTextListener(editText -> {
            // If the preference is empty, populate the EditText with the default number instead.
            if (TextUtils.isEmpty(getText()) && !TextUtils.isEmpty(mDefaultPhoneNumber)) {
                final BidiFormatter bidiFormatter1 = BidiFormatter.getInstance();
                final String phoneNumber1 = bidiFormatter1.unicodeWrap
                        (PhoneUtils.get(mSubId).getCanonicalBySystemLocale(mDefaultPhoneNumber),
                                TextDirectionHeuristicsCompat.LTR);
                editText.setText(phoneNumber1);
            }
            editText.setInputType(InputType.TYPE_CLASS_PHONE);
        });

        setOnPreferenceChangeListener((preference, newValue) -> {
            if (mDefaultPhoneNumber != null) {
                final PhoneUtils phoneUtils = PhoneUtils.get(mSubId);
                final String phoneNumber2 = phoneUtils.getCanonicalBySystemLocale((String) newValue);
                final String defaultPhoneNumber = phoneUtils.getCanonicalBySystemLocale(
                        mDefaultPhoneNumber);

                // If the new value is the default, clear the preference.
                if (phoneNumber2.equals(defaultPhoneNumber)) {
                    setText("");
                }
            }
            return true;
        });

        super.onBindViewHolder(holder);
    }

    @Nullable
    @Override
    public CharSequence getSummary() {
        // Show the preference value if it's set, or the default number if not.
        // If we don't have a default, fall back to a static string (e.g. Unknown).
        String value = getText();
        if (TextUtils.isEmpty(value)) {
            value = mDefaultPhoneNumber;
        }
        final String displayValue = (!TextUtils.isEmpty(value))
                ? PhoneUtils.get(mSubId).formatForDisplay(value)
                : getContext().getString(R.string.unknown_phone_number_pref_display_value);
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        return bidiFormatter.unicodeWrap
                (displayValue, TextDirectionHeuristicsCompat.LTR);
    }
}
