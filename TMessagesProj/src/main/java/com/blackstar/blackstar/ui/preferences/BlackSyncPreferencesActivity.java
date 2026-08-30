/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.ui.preferences;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blackstar.messenger.preferences.BasePreferencesActivity;
import com.blackstar.blackstar.BlackConfig;
import com.blackstar.blackstar.BlackConstants;
import com.blackstar.blackstar.BlackUtils;
import com.blackstar.blackstar.sync.BlackSyncConfig;
import com.blackstar.blackstar.sync.BlackSyncController;
import com.blackstar.blackstar.sync.BlackSyncState;
import com.blackstar.blackstar.ui.preferences.utils.BlackUi;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;

public class BlackSyncPreferencesActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {


    private int generalHeaderRow;
    private int serverURLRow;
    private int serverTokenRow;
    private int syncEnabledRow;
    private int generalDividerRow;

    private int visitProfileRow;
    private int forceSyncRow;
    private int visitProfileDividerRow;

    private int debugHeaderRow;
    private int ayuSyncStatusRow;
    private int registerStatusCodeRow;
    private int deviceIdentifierRow;
    private int lastReceivedEventRow;
    private int lastSentEventRow;
    private int useSecureConnectionRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        generalHeaderRow = newRow();
        serverURLRow = newRow();
        serverTokenRow = newRow();
        syncEnabledRow = newRow();
        generalDividerRow = newRow();

        visitProfileRow = newRow();
        forceSyncRow = newRow();
        visitProfileDividerRow = newRow();

        debugHeaderRow = newRow();
        ayuSyncStatusRow = newRow();
        registerStatusCodeRow = newRow();
        deviceIdentifierRow = newRow();
        lastSentEventRow = newRow();
        lastReceivedEventRow = newRow();
        useSecureConnectionRow = newRow();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, BlackConstants.AYUSYNC_STATE_CHANGED);
        NotificationCenter.getGlobalInstance().addObserver(this, BlackConstants.AYUSYNC_LAST_SENT_CHANGED);
        NotificationCenter.getGlobalInstance().addObserver(this, BlackConstants.AYUSYNC_LAST_RECEIVED_CHANGED);
        NotificationCenter.getGlobalInstance().addObserver(this, BlackConstants.AYUSYNC_REGISTER_STATUS_CODE_CHANGED);
        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == BlackConstants.AYUSYNC_STATE_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(ayuSyncStatusRow);
            }
        } else if (id == BlackConstants.AYUSYNC_LAST_SENT_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(lastSentEventRow);
            }
        } else if (id == BlackConstants.AYUSYNC_LAST_RECEIVED_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(lastReceivedEventRow);
            }
        } else if (id == BlackConstants.AYUSYNC_REGISTER_STATUS_CODE_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(registerStatusCodeRow);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, BlackConstants.AYUSYNC_STATE_CHANGED);
        NotificationCenter.getGlobalInstance().removeObserver(this, BlackConstants.AYUSYNC_LAST_SENT_CHANGED);
        NotificationCenter.getGlobalInstance().removeObserver(this, BlackConstants.AYUSYNC_LAST_RECEIVED_CHANGED);
        NotificationCenter.getGlobalInstance().removeObserver(this, BlackConstants.AYUSYNC_REGISTER_STATUS_CODE_CHANGED);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == serverURLRow) {
            BlackUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.BlackSyncServerURL),
                    BlackConfig::getSyncServerURL,
                    "syncServerURL",
                    BlackConstants.DEFAULT_AYUSYNC_SERVER,
                    s -> {
                        BlackSyncController.nullifyInstance();
                        BlackSyncController.create();
                    },
                    s -> {
                        if (s.contains("http") || s.contains("ws")) {
                            s = s.substring(s.indexOf("://") + 3);
                        }

                        if (s.endsWith("/")) {
                            s = s.substring(0, s.length() - 1);
                        }

                        return s;
                    }
            );
        } else if (position == serverTokenRow) {
            BlackUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.BlackSyncServerToken),
                    BlackConfig::getSyncServerToken,
                    "syncServerToken",
                    "",
                    s -> {
                        BlackSyncController.nullifyInstance();
                        BlackSyncController.create();
                    }
            );
        } else if (position == syncEnabledRow) {
            BlackConfig.editor.putBoolean("syncEnabled", BlackConfig.syncEnabled ^= true).apply();
            ((TextCheckCell) view).setChecked(BlackConfig.syncEnabled);

            BlackSyncController.nullifyInstance();
            BlackSyncController.create();
        } else if (position == visitProfileRow) {
            Browser.openUrl(getParentActivity(), BlackSyncConfig.getProfileURL());
        } else if (position == forceSyncRow) {
            BlackSyncController.getInstance().forceSync();
        } else if (position == deviceIdentifierRow) {
            AndroidUtilities.addToClipboard(BlackUtils.getDeviceIdentifier());
            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BlackSyncIdentifierCopied)).show();
        } else if (position == useSecureConnectionRow) {
            BlackConfig.editor.putBoolean("useSecureConnection", BlackConfig.useSecureConnection ^= true).apply();
            ((TextCheckCell) view).setChecked(BlackConfig.useSecureConnection);

            BlackSyncController.nullifyInstance();
            BlackSyncController.create();
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.BlackSyncHeader);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == serverURLRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncServerURL), BlackConfig.getSyncServerURL(), true);
                    } else if (position == serverTokenRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncServerToken), BlackConfig.getSyncServerToken(), true);
                    } else if (position == visitProfileRow) {
                        textCell.setText(LocaleController.getString(R.string.BlackSyncVisitProfile), true);
                    } else if (position == forceSyncRow) {
                        textCell.setText(LocaleController.getString(R.string.BlackSyncForceSync), false);
                    } else if (position == ayuSyncStatusRow) {
                        var status = BlackSyncState.getConnectionStateString();

                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncStatusTitle), status, true);
                    } else if (position == deviceIdentifierRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncDeviceIdentifier), BlackUtils.getDeviceIdentifier(), true);
                    } else if (position == lastSentEventRow) {
                        var last = BlackSyncState.getLastSent() != 0
                                ? LocaleController.formatDateAudio(BlackSyncState.getLastSent(), true)
                                : LocaleController.getString(R.string.BlackSyncLastEventNever);

                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncLastEventSent), last, true);
                    } else if (position == lastReceivedEventRow) {
                        var last = BlackSyncState.getLastReceived() != 0
                                ? LocaleController.formatDateAudio(BlackSyncState.getLastReceived(), true)
                                : LocaleController.getString(R.string.BlackSyncLastEventNever);

                        textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncLastEventReceived), last, true);
                    } else if (position == registerStatusCodeRow) {
                        var val = BlackSyncState.getRegisterStatusCode();
                        if (val == 0) {
                            textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncRegisterStatusCode), "?", true);
                        } else {
                            textCell.setTextAndValue(LocaleController.getString(R.string.BlackSyncRegisterStatusCode), String.valueOf(val), true);
                        }
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalHeaderRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == debugHeaderRow) {
                        headerCell.setText(LocaleController.getString("SettingsDebug", R.string.SettingsDebug));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == syncEnabledRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.BlackSyncEnable), BlackConfig.syncEnabled, false);
                    } else if (position == useSecureConnectionRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.BlackSyncUseSecureConnection), BlackConfig.useSecureConnection, false);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == generalDividerRow ||
                            position == visitProfileDividerRow
            ) {
                return 1;
            } else if (
                    position == serverURLRow ||
                            position == serverTokenRow ||
                            position == visitProfileRow ||
                            position == forceSyncRow ||
                            position == ayuSyncStatusRow ||
                            position == deviceIdentifierRow ||
                            position == lastSentEventRow ||
                            position == lastReceivedEventRow ||
                            position == registerStatusCodeRow
            ) {
                return 2;
            } else if (
                    position == generalHeaderRow ||
                            position == debugHeaderRow
            ) {
                return 3;
            }
            return 5;
        }
    }
}
