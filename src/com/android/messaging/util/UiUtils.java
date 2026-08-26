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

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding;
import com.android.messaging.ui.UIIntents;

import dagger.hilt.android.EntryPointAccessors;

public class UiUtils {
    /** Show a simple toast at the bottom */
    public static void showToastAtBottom(final int messageId) {
        UiUtils.showToastAtBottom(getApplicationContext().getString(messageId));
    }

    /** Show a simple toast at the bottom */
    public static void showToastAtBottom(final String message) {
        final Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    private static Context getApplicationContext() {
        return Factory.get().getApplicationContext();
    }

    /**
     * Check if the activity needs to be redirected to onboarding, which covers both the
     * once-per-version SMS warning and the permission check. Onboarding is a destination inside
     * the single-Activity host, so redirecting means launching the host and finishing the caller.
     * @return true if {@link Activity#finish()} was called because redirection was performed
     */
    public static boolean redirectToOnboardingIfNeeded(final Activity activity) {
        if (!shouldShowOnboarding()) {
            // No redirect performed
            return false;
        }

        UIIntents.get().launchConversationListActivity(activity);
        activity.finish();
        return true;
    }

    private static boolean shouldShowOnboarding() {
        final Context context = Factory.get().getApplicationContext();
        return EntryPointAccessors
                .fromApplication(context, ShouldShowOnboarding.Provider.class)
                .shouldShowOnboarding()
                .invoke();
    }

    public static RemoteViews getWidgetMissingPermissionView(final Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_missing_permission);
    }
}
