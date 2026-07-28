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
import android.content.ContextWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding;
import com.android.messaging.ui.UIIntents;

import dagger.hilt.android.EntryPointAccessors;

import androidx.annotation.Nullable;

public class UiUtils {
    private static final Interpolator DEFAULT_INTERPOLATOR = new CubicBezierInterpolator(
            0.4f, 0.0f, 0.2f, 1.0f);

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

    /** Generic duration for revealing/hiding a view in ms */
    private static int getRevealAnimationDuration() {
        return getApplicationContext().getResources().getInteger(
                R.integer.reveal_view_animation_duration);
    }

    /**
     * Reveals/Hides a view with a scale animation from view center.
     * @param view the view to animate
     * @param desiredVisibility desired visibility (e.g. View.GONE) for the animated view.
     * @param onFinishRunnable an optional runnable called at the end of the animation
     */
    public static void revealOrHideViewWithAnimation(final View view, final int desiredVisibility,
            @Nullable final Runnable onFinishRunnable) {
        final boolean needAnimation = view.getVisibility() != desiredVisibility;
        if (needAnimation) {
            final float fromScale = desiredVisibility == View.VISIBLE ? 0F : 1F;
            final float toScale = desiredVisibility == View.VISIBLE ? 1F : 0F;
            final ScaleAnimation showHideAnimation =
                    new ScaleAnimation(fromScale, toScale, fromScale, toScale,
                            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                            ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            showHideAnimation.setDuration(getRevealAnimationDuration());
            showHideAnimation.setInterpolator(DEFAULT_INTERPOLATOR);
            showHideAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    if (onFinishRunnable != null) {
                        // Rather than running this immediately, we post it to happen next so that
                        // the animation will be completed so that the view can be detached from
                        // it's window.  Otherwise, we may leak memory.
                        ThreadUtil.getMainThreadHandler().post(onFinishRunnable);
                    }
                }
            });
            view.clearAnimation();
            view.startAnimation(showHideAnimation);
            // We are playing a view Animation; unlike view property animations, we can commit the
            // visibility immediately instead of waiting for animation end.
            view.setVisibility(desiredVisibility);
        } else if (onFinishRunnable != null) {
            // Make sure onFinishRunnable is always executed.
            ThreadUtil.getMainThreadHandler().post(onFinishRunnable);
        }
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

    /**
     * Get the activity that's hosting the view, typically casting view.getContext() as an Activity
     * is sufficient, but sometimes the context is a context wrapper, in which case we need to case
     * the base context
     */
    public static Activity getActivity(final View view) {
        if (view == null) {
            return null;
        }
        return getActivity(view.getContext());
    }

    /**
     * Get the activity for the supplied context, typically casting context as an Activity
     * is sufficient, but sometimes the context is a context wrapper, in which case we need to case
     * the base context
     */
    public static Activity getActivity(final Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }

        // We've hit a non-activity context such as an app-context
        return null;
    }

    public static RemoteViews getWidgetMissingPermissionView(final Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_missing_permission);
    }
}
