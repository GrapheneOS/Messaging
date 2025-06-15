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
package com.android.messaging.ui.conversationlist;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.messaging.R;
import com.android.messaging.util.DebugUtils;

import androidx.appcompat.app.ActionBar;

public class ArchivedConversationListActivity extends AbstractConversationListActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ConversationListFragment fragment =
                ConversationListFragment.createArchivedConversationListFragment();
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
        invalidateActionBar();
    }

    protected void updateActionBar(ActionBar actionBar) {
        actionBar.setTitle(getString(R.string.archived_activity_title));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(
                        R.color.archived_conversation_action_bar_background_color_dark)));
        actionBar.show();
        super.updateActionBar(actionBar);
    }

    @Override
    public void onBackPressed() {
        if (isInConversationListSelectMode()) {
            exitMultiSelectState();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.archived_conversation_list_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_debug_options);
        if (item != null) {
            final boolean enableDebugItems = DebugUtils.isDebugEnabled();
            item.setVisible(enableDebugItems).setEnabled(enableDebugItems);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_debug_options) {
            onActionBarDebug();
            return true;
        }
        if (itemId == android.R.id.home) {
            onActionBarHome();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActionBarHome() {
        onBackPressed();
    }

    @Override
    public boolean isSwipeAnimatable() {
        return false;
    }
}
