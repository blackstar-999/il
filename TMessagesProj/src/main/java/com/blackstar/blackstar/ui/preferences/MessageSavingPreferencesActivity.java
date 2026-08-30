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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blackstar.messenger.preferences.BasePreferencesActivity;
import com.blackstar.blackstar.BlackConfig;
import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.*;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class MessageSavingPreferencesActivity extends BasePreferencesActivity {

    private static final int TOGGLE_BUTTON_VIEW = 1000;

    private int generalHeaderRow;
    private int saveMediaRow;
    private int saveFormatting;
    private int saveReactions;
    private int saveForBots;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        generalHeaderRow = newRow();
        saveMediaRow = newRow();
        saveFormatting = newRow();
        saveReactions = newRow();
        saveForBots = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == saveMediaRow) {
            TextCheckCell2 checkCell = (TextCheckCell2) view;
            if (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {
                BlackConfig.editor.putBoolean("saveMedia", BlackConfig.saveMedia ^= true).apply();
                checkCell.setChecked(BlackConfig.saveMedia);
            } else if (BlackConfig.saveMedia) {
                showBottomSheet();
            }
        } else if (position == saveFormatting) {
            BlackConfig.editor.putBoolean("saveFormatting", BlackConfig.saveFormatting ^= true).apply();
            ((TextCheckCell) view).setChecked(BlackConfig.saveFormatting);
        } else if (position == saveReactions) {
            BlackConfig.editor.putBoolean("saveReactions", BlackConfig.saveReactions ^= true).apply();
            ((TextCheckCell) view).setChecked(BlackConfig.saveReactions);
        } else if (position == saveForBots) {
            BlackConfig.editor.putBoolean("saveForBots", BlackConfig.saveForBots ^= true).apply();
            ((TextCheckCell) view).setChecked(BlackConfig.saveForBots);
        }
    }

    @Override
    public void onResume() {
        updateRowsId();

        super.onResume();
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.MessageSavingBtn);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private void showBottomSheet() {
        if (getParentActivity() == null) {
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setApplyTopPadding(false);
        builder.setApplyBottomPadding(false);
        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setCustomView(linearLayout);

        HeaderCell headerCell = new HeaderCell(getParentActivity(), Theme.key_dialogTextBlue2, 21, 15, false);
        headerCell.setText(LocaleController.getString(R.string.MessageSavingSaveMedia).toUpperCase());
        linearLayout.addView(headerCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextCheckBoxCell[] cells = new TextCheckBoxCell[5];
        for (int a = 0; a < cells.length; a++) {
            TextCheckBoxCell checkBoxCell = cells[a] = new TextCheckBoxCell(getParentActivity(), true, false);
            if (a == 0) {
                cells[a].setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChats), BlackConfig.saveMediaInPrivateChats, true);
            } else if (a == 1) {
                cells[a].setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPublicChannels), BlackConfig.saveMediaInPublicChannels, true);
            } else if (a == 2) {
                cells[a].setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChannels), BlackConfig.saveMediaInPrivateChannels, true);
            } else if (a == 3) {
                cells[a].setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPublicGroups), BlackConfig.saveMediaInPublicGroups, true);
            } else { // a == 4
                cells[a].setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateGroups), BlackConfig.saveMediaInPrivateGroups, true);
            }
            cells[a].setBackgroundDrawable(Theme.getSelectorDrawable(false));
            cells[a].setOnClickListener(v -> {
                if (!v.isEnabled()) {
                    return;
                }
                checkBoxCell.setChecked(!checkBoxCell.isChecked());
            });
            linearLayout.addView(cells[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50));
        }

        FrameLayout buttonsLayout = new FrameLayout(getParentActivity());
        buttonsLayout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        linearLayout.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));

        TextView textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(LocaleController.getString("Cancel", R.string.Cancel).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.LEFT));
        textView.setOnClickListener(v14 -> builder.getDismissRunnable().run());

        textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(LocaleController.getString("Save", R.string.Save).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.RIGHT));
        textView.setOnClickListener(v1 -> {
            BlackConfig.saveMediaInPrivateChats = cells[0].isChecked();
            BlackConfig.saveMediaInPublicChannels = cells[1].isChecked();
            BlackConfig.saveMediaInPrivateChannels = cells[2].isChecked();
            BlackConfig.saveMediaInPublicGroups = cells[3].isChecked();
            BlackConfig.saveMediaInPrivateGroups = cells[4].isChecked();

            BlackConfig.editor.putBoolean("saveMediaInPrivateChats", BlackConfig.saveMediaInPrivateChats).apply();
            BlackConfig.editor.putBoolean("saveMediaInPublicChannels", BlackConfig.saveMediaInPublicChannels).apply();
            BlackConfig.editor.putBoolean("saveMediaInPrivateChannels", BlackConfig.saveMediaInPrivateChannels).apply();
            BlackConfig.editor.putBoolean("saveMediaInPublicGroups", BlackConfig.saveMediaInPublicGroups).apply();
            BlackConfig.editor.putBoolean("saveMediaInPrivateGroups", BlackConfig.saveMediaInPrivateGroups).apply();

            builder.getDismissRunnable().run();
        });
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            if (viewType == TOGGLE_BUTTON_VIEW) {
                var view = new NotificationsCheckCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                return new RecyclerListView.Holder(view);
            }
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.General));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    if (position == saveFormatting) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveFormatting), BlackConfig.saveFormatting, true);
                        textCheckCell.setEnabled(true, null);
                    } else if (position == saveReactions) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveReactions), BlackConfig.saveReactions, true);
                        textCheckCell.setEnabled(true, null);
                    } else if (position == saveForBots) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveForBots), BlackConfig.saveForBots, false);
                        textCheckCell.setEnabled(true, null);
                    }
                    break;
                case 18:
                    TextCheckCell2 checkCell = (TextCheckCell2) holder.itemView;
                    if (position == saveMediaRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.MessageSavingSaveMedia), LocaleController.getString(R.string.MessageSavingSaveMediaHint), BlackConfig.saveMedia, false, true);
                    }
                    checkCell.getCheckBox().setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
                    checkCell.getCheckBox().setDrawIconType(0);
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == generalHeaderRow
            ) {
                return 3;
            } else if (
                    position == saveMediaRow
            ) {
                return 18;
            }
            return 5;
        }
    }
}
