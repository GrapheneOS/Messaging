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

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.android.messaging.R;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachmentSaveTask extends SafeAsyncTask<Void, Void, Void> {
    private final Context mContext;
    private final List<AttachmentToSave> mAttachmentsToSave = new ArrayList<>();

    public AttachmentSaveTask(final Context context, final Uri contentUri,
            final String contentType) {
        mContext = context;
        addAttachmentToSave(contentUri, contentType);
    }

    public AttachmentSaveTask(final Context context) {
        mContext = context;
    }

    public void addAttachmentToSave(final Uri contentUri, final String contentType) {
        mAttachmentsToSave.add(new AttachmentToSave(contentUri, contentType));
    }

    public int getAttachmentCount() {
        return mAttachmentsToSave.size();
    }

    @Override
    protected Void doInBackgroundTimed(final Void... arg) {
        final File appDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES),
                mContext.getResources().getString(R.string.app_name));
        final File downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        for (final AttachmentToSave attachment : mAttachmentsToSave) {
            final boolean isImageOrVideo = ContentType.isImageType(attachment.contentType)
                    || ContentType.isVideoType(attachment.contentType);
            attachment.persistedUri = UriUtil.persistContent(attachment.uri,
                    isImageOrVideo ? appDir : downloadDir, attachment.contentType);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Void result) {
        int failCount = 0;
        int imageCount = 0;
        int videoCount = 0;
        int otherCount = 0;
        for (final AttachmentToSave attachment : mAttachmentsToSave) {
            if (attachment.persistedUri == null) {
                failCount++;
                continue;
            }

            final Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanFileIntent.setData(attachment.persistedUri);
            mContext.sendBroadcast(scanFileIntent);

            if (ContentType.isImageType(attachment.contentType)) {
                imageCount++;
            } else if (ContentType.isVideoType(attachment.contentType)) {
                videoCount++;
            } else {
                otherCount++;
                final DownloadManager downloadManager =
                        (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                final String filePath = attachment.persistedUri.getPath();
                final File file = new File(filePath);

                if (file.exists()) {
                    downloadManager.addCompletedDownload(
                            file.getName() /* title */,
                            mContext.getString(
                                    R.string.attachment_file_description) /* description */,
                            true /* isMediaScannerScannable */,
                            attachment.contentType,
                            file.getAbsolutePath(),
                            file.length(),
                            false /* showNotification */);
                }
            }
        }

        final String message;
        if (failCount > 0) {
            message = mContext.getResources().getQuantityString(
                    R.plurals.attachment_save_error, failCount, failCount);
        } else {
            int messageId = R.plurals.attachments_saved;
            if (otherCount > 0) {
                if (imageCount + videoCount == 0) {
                    messageId = R.plurals.attachments_saved_to_downloads;
                }
            } else {
                if (videoCount == 0) {
                    messageId = R.plurals.photos_saved_to_album;
                } else if (imageCount == 0) {
                    messageId = R.plurals.videos_saved_to_album;
                } else {
                    messageId = R.plurals.attachments_saved_to_album;
                }
            }
            final String appName = mContext.getResources().getString(R.string.app_name);
            final int count = imageCount + videoCount + otherCount;
            message = mContext.getResources().getQuantityString(messageId, count, count, appName);
        }
        UiUtils.showToastAtBottom(message);
    }

    private static class AttachmentToSave {
        public final Uri uri;
        public final String contentType;
        public Uri persistedUri;

        AttachmentToSave(final Uri uri, final String contentType) {
            this.uri = uri;
            this.contentType = contentType;
        }
    }
}
