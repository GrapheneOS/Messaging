package com.android.messaging.datamodel.data

import android.database.Cursor
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader

/**
 * Basic wrapper for [LoaderManager.LoaderCallbacks] so we can ensure a
 * consistent position for the cursor provided in [onLoadFinish].
 */
abstract class CursorLoaderCallbacks : LoaderManager.LoaderCallbacks<Cursor> {
    abstract fun onLoadFinish(loader: Loader<Cursor>, data: Cursor?)

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data?.moveToPosition(-1)
        onLoadFinish(loader, data)
    }
}
