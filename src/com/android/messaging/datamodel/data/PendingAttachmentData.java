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
package com.android.messaging.datamodel.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;

/**
 * Represents a "pending" message part that acts as a placeholder for the actual attachment being
 * loaded. It handles the task to load and persist the attachment from a Uri to local scratch
 * folder. This item is not persisted to the database.
 */
public class PendingAttachmentData extends MessagePartData {
    /** The pending state. This is the initial state where we haven't started loading yet */
    public static final int STATE_PENDING = 0;

    /** The state for when we are currently loading the attachment to the scratch space */
    public static final int STATE_LOADING = 1;

    /** The attachment has been successfully loaded and no longer pending */
    public static final int STATE_LOADED = 2;

    /** The attachment failed to load */
    public static final int STATE_FAILED = 3;

    /** The current state of the pending attachment. Refer to the STATE_* states above */
    private int mCurrentState;

    /**
     * Create a new instance of PendingAttachmentData with an output Uri.
     * @param sourceUri the source Uri of the attachment. The Uri maybe temporary or remote,
     * so we need to persist it to local storage.
     */
    protected PendingAttachmentData(final String caption, final String contentType,
            @NonNull final Uri sourceUri, final int width, final int height,
            final boolean onlySingleAttachment) {
        super(caption, contentType, sourceUri, width, height, onlySingleAttachment);
        mCurrentState = STATE_PENDING;
    }

    /**
     * Creates a pending attachment data that is able to load from the given source uri and
     * persist the media resource locally in the scratch folder.
     */
    public static PendingAttachmentData createPendingAttachmentData(final String contentType,
            final Uri sourceUri) {
        return createPendingAttachmentData(null, contentType, sourceUri, UNSPECIFIED_SIZE,
                UNSPECIFIED_SIZE);
    }

    public static PendingAttachmentData createPendingAttachmentData(final String caption,
            final String contentType, final Uri sourceUri, final int width, final int height) {
        Assert.isTrue(ContentType.isMediaType(contentType));
        return new PendingAttachmentData(caption, contentType, sourceUri, width, height,
                false /*onlySingleAttachment*/);
    }

    public static PendingAttachmentData createPendingAttachmentData(final String caption,
            final String contentType, final Uri sourceUri, final int width, final int height,
            final boolean onlySingleAttachment) {
        Assert.isTrue(ContentType.isMediaType(contentType));
        return new PendingAttachmentData(caption, contentType, sourceUri, width, height,
                onlySingleAttachment);
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    protected PendingAttachmentData(final Parcel in) {
        super(in);
        mCurrentState = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mCurrentState);
    }

    public static final Parcelable.Creator<PendingAttachmentData> CREATOR
        = new Parcelable.Creator<PendingAttachmentData>() {
            @Override
            public PendingAttachmentData createFromParcel(final Parcel in) {
                return new PendingAttachmentData(in);
            }

            @Override
            public PendingAttachmentData[] newArray(final int size) {
                return new PendingAttachmentData[size];
            }
        };
}
