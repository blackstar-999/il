/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blackstar.blackstar.BlackConstants;
import com.blackstar.blackstar.database.entities.EditedMessage;
import com.blackstar.blackstar.messages.BlackMessagesController;
import com.blackstar.blackstar.proprietary.BlackMessageUtils;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlackMessageHistory extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private final MessageObject messageObject;
    private List<EditedMessage> messages;
    private int rowCount;
    private RecyclerListView listView;

    public BlackMessageHistory(MessageObject messageObject) {
        this.messageObject = messageObject;

        updateHistory();
    }

    private void updateHistory() {
        var messagesController = BlackMessagesController.getInstance();
        messages = messagesController.getRevisions(getUserConfig().clientUserId, messageObject.messageOwner.dialog_id, messageObject.messageOwner.id);
        rowCount = messages.size();
    }

    @Override
    public View createView(Context context) {
        var firstMsg = messages.get(0);
        var peer = getMessagesController().getUserOrChat(firstMsg.dialogId);
        // todo: check sender of the message

        String name;
        if (peer == null) {
            name = "?"; // wtf
        } else if (peer instanceof TLRPC.User) {
            name = ((TLRPC.User) peer).first_name;
        } else if (peer instanceof TLRPC.Chat) {
            name = ((TLRPC.Chat) peer).title;
        } else {
            name = LocaleController.getString("EditsHistoryTitle", R.string.EditsHistoryTitle);
        }

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(name);
        actionBar.setSubtitle(String.valueOf(firstMsg.messageId));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        LinearLayoutManager layoutManager;
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        ListAdapter adapter;
        listView.setAdapter(adapter = new BlackMessageHistory.ListAdapter(context));

        return fragmentView;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, BlackConstants.MESSAGE_EDITED_NOTIFICATION);

        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == BlackConstants.MESSAGE_EDITED_NOTIFICATION) {
            var dialogId = (long) args[0];
            var messageId = (int) args[1];

            if (dialogId == messageObject.messageOwner.dialog_id && messageId == messageObject.messageOwner.id) {
                updateHistory();
                listView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, BlackConstants.MESSAGE_EDITED_NOTIFICATION);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new BlackMessageCell(context, getParentActivity(), BlackMessageHistory.this);
            } else {
                view = null;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                var ayuMessageDetailCell = (BlackMessageCell) holder.itemView;

                var editedMessage = messages.get(position);
                var msg = createMessageObject(editedMessage);

                ayuMessageDetailCell.setMessageObject(msg, null, false, false);
                ayuMessageDetailCell.setEditedMessage(editedMessage);
                ayuMessageDetailCell.setId(position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position >= 0 && position < messages.size() ? 1 : 0;
        }

        private MessageObject createMessageObject(EditedMessage editedMessage) {
            // shamefully copied from Extera's sticker size preview
            var msg = new TLRPC.TL_message();
            BlackMessageUtils.map(editedMessage, msg, currentAccount);
            BlackMessageUtils.mapMedia(editedMessage, msg);

            msg.date = editedMessage.entityCreateDate;
            msg.edit_hide = true;

            // let file be downloaded
            if (editedMessage.documentType == BlackConstants.DOCUMENT_TYPE_FILE && Objects.equals(editedMessage.mediaPath, FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner).getAbsolutePath())) {
                msg.media.document = messageObject.messageOwner.media.document;
            }

            // fix reply state
            if (messageObject.messageOwner.replyMessage != null) {
                msg.replyMessage = messageObject.messageOwner.replyMessage;
                msg.reply_to = messageObject.messageOwner.reply_to;
            }

            return new MessageObject(getCurrentAccount(), msg, false, true);
        }
    }
}