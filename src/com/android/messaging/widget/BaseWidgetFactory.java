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

package com.android.messaging.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Binder;
import android.os.Parcel;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.Nullable;

import com.android.messaging.R;
import com.android.messaging.datamodel.media.AvatarGroupRequestDescriptor;
import com.android.messaging.datamodel.media.AvatarRequestDescriptor;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.ImageResource;
import com.android.messaging.datamodel.media.MediaRequest;
import com.android.messaging.datamodel.media.MediaResourceManager;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remote Views Factory for Bugle Widget.
 */
abstract class BaseWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    protected static final String TAG = LogUtil.BUGLE_WIDGET_TAG;

    protected static final int MAX_ITEMS_TO_SHOW = 25;

    /**
     * The rows go to the system in a single binder transaction, which fails past 1 MB.
     */
    private static final int MAX_ITEMS_PARCEL_SIZE = 500_000;

    /**
     * Lock to avoid race condition between widgets.
     */
    protected static final Object sWidgetLock = new Object();

    protected final Context mContext;
    protected final int mAppWidgetId;
    protected boolean mShouldShowViewMore;
    protected Cursor mCursor;
    protected final AppWidgetManager mAppWidgetManager;
    protected int mIconSize;
    private final Map<Uri, Bitmap> mAvatarBitmaps = new HashMap<>();
    private long mBitmapByteCount;

    public BaseWidgetFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "BaseWidgetFactory intent: " + intent + "widget id: " + mAppWidgetId);
        }
        mIconSize = (int) context.getResources()
                .getDimension(R.dimen.contact_icon_view_normal_size);

    }

    @Override
    public void onCreate() {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "onCreate");
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "onDestroy");
        }
        synchronized (sWidgetLock) {
            if (mCursor != null && !mCursor.isClosed()) {
                mCursor.close();
                mCursor = null;
            }
        }
    }

    @Override
    public void onDataSetChanged() {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "onDataSetChanged");
        }
        synchronized (sWidgetLock) {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            mAvatarBitmaps.clear();
            mBitmapByteCount = 0;
            final long token = Binder.clearCallingIdentity();
            try {
                mCursor = doQuery();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    protected abstract Cursor doQuery();

    /**
     * Returns the number of items that should be shown in the widget list.  This method also
     * updates the boolean that indicates whether the "show more" item should be shown.
     * @return the number of items to be displayed in the list.
     */
    @Override
    public int getCount() {
        synchronized (sWidgetLock) {
            if (mCursor == null) {
                if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
                    LogUtil.v(TAG, "getCount: 0");
                }
                return 0;
            }
            final int count = getItemCount();
            if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
                LogUtil.v(TAG, "getCount: " + count);
            }
            mShouldShowViewMore = count < mCursor.getCount();
            return count + (mShouldShowViewMore ? 1 : 0);
        }
    }

    /**
     * Returns the number of messages that should be shown in the widget.  This method
     * doesn't update the boolean that indicates whether the "show more" item should be included
     * in the list.
     * @return
     */
    protected int getItemCount() {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "getItemCount: " + mCursor.getCount());
        }
        return Math.min(mCursor.getCount(), MAX_ITEMS_TO_SHOW);
    }

    /*
     * Make the given text bold if the item is unread
     */
    protected CharSequence boldifyIfUnread(CharSequence text, final boolean unread) {
        if (!unread) {
            return text;
        }
        final SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    protected Bitmap getAvatarBitmap(final Uri avatarUri) {
        if (mAvatarBitmaps.containsKey(avatarUri)) {
            return mAvatarBitmaps.get(avatarUri);
        }
        final String avatarType = avatarUri == null ?
                null : AvatarUriUtil.getAvatarType(avatarUri);
        ImageRequestDescriptor descriptor;
        if (AvatarUriUtil.TYPE_GROUP_URI.equals(avatarType)) {
            descriptor = new AvatarGroupRequestDescriptor(avatarUri, mIconSize, mIconSize);
        } else {
            descriptor = new AvatarRequestDescriptor(avatarUri, mIconSize, mIconSize);
        }

        final Bitmap avatar = loadBitmap(descriptor);
        mAvatarBitmaps.put(avatarUri, avatar);
        return avatar;
    }

    protected Bitmap loadBitmap(final ImageRequestDescriptor descriptor) {
        final MediaRequest<ImageResource> imageRequest =
                descriptor.buildSyncMediaRequest(mContext);
        final Bitmap bitmap = copyBitmapAndRelease(
                MediaResourceManager.get().requestMediaResourceSync(imageRequest));
        if (bitmap != null) {
            mBitmapByteCount += bitmap.getAllocationByteCount();
        }
        return bitmap;
    }

    /**
     * Returns a copy of the resource's bitmap and releases the resource. The host asks for every
     * list item before it shares their bitmaps, and by then the media cache may have recycled or
     * reused a bitmap it still owns.
     */
    protected static Bitmap copyBitmapAndRelease(final ImageResource imageResource) {
        if (imageResource == null) {
            return null;
        }
        try {
            final Bitmap bitmap = imageResource.getBitmap();
            return bitmap == null ? null : bitmap.copy(bitmap.getConfig(), false /* isMutable */);
        } finally {
            imageResource.release();
        }
    }

    /**
     * @return the "View more messages" view. When the user taps this item, they're
     * taken to the conversation in Bugle.
     */
    abstract protected RemoteViews getViewMoreItemsView();

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected abstract int getMainLayoutId();

    @Nullable
    protected abstract RemoteViews getItemView(int cursorPosition);

    protected abstract boolean isNewestItemLast();

    RemoteViews.RemoteCollectionItems buildItems(final long maxBitmapByteCount) {
        synchronized (sWidgetLock) {
            final Parcel sizeParcel = Parcel.obtain();
            try {
                onDataSetChanged();
                final List<RemoteViews> rows = buildRows(maxBitmapByteCount, sizeParcel);
                if (isNewestItemLast()) {
                    Collections.reverse(rows);
                }
                if (mCursor != null && rows.size() < mCursor.getCount()) {
                    rows.add(isNewestItemLast() ? 0 : rows.size(), getViewMoreItemsView());
                }
                final RemoteViews.RemoteCollectionItems.Builder items =
                        new RemoteViews.RemoteCollectionItems.Builder()
                                .setHasStableIds(hasStableIds())
                                .setViewTypeCount(getViewTypeCount());
                for (int position = 0; position < rows.size(); position++) {
                    items.addItem(getItemId(position), rows.get(position));
                }
                return items.build();
            } finally {
                sizeParcel.recycle();
                onDestroy();
            }
        }
    }

    private List<RemoteViews> buildRows(final long maxBitmapByteCount, final Parcel sizeParcel) {
        final int itemCount = mCursor == null ? 0 : getItemCount();
        final List<RemoteViews> rows = new ArrayList<>(itemCount + 1);
        for (int position = 0; position < itemCount; position++) {
            final RemoteViews row = getItemView(position);
            if (row == null) {
                break;
            }
            row.writeToParcel(sizeParcel, 0 /* flags */);
            if (mBitmapByteCount > maxBitmapByteCount
                    || sizeParcel.dataSize() > MAX_ITEMS_PARCEL_SIZE) {
                LogUtil.w(TAG, "Widget " + mAppWidgetId + " shows " + position + " of "
                        + itemCount + " items, the rest don't fit in one update");
                break;
            }
            rows.add(row);
        }
        return rows;
    }
}
