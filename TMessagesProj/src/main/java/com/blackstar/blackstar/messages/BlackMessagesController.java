/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.messages;

import android.os.Environment;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.Log;
import com.blackstar.blackstar.BlackConfig;
import com.blackstar.blackstar.BlackConstants;
import com.blackstar.blackstar.database.BlackData;
import com.blackstar.blackstar.database.dao.DeletedMessageDao;
import com.blackstar.blackstar.database.dao.EditedMessageDao;
import com.blackstar.blackstar.database.entities.DeletedMessage;
import com.blackstar.blackstar.database.entities.DeletedMessageFull;
import com.blackstar.blackstar.database.entities.DeletedMessageReaction;
import com.blackstar.blackstar.database.entities.EditedMessage;
import com.blackstar.blackstar.proprietary.BlackMessageUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BlackMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    public static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BlackConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static BlackMessagesController instance;
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private BlackMessagesController() {
        initializeAttachmentsFolder();

        editedMessageDao = BlackData.getEditedMessageDao();
        deletedMessageDao = BlackData.getDeletedMessageDao();
    }

    private static void initializeAttachmentsFolder() {
        if (!attachmentsPath.exists()) {
            attachmentsPath.mkdirs();
            try {
                new File(attachmentsPath, ".nomedia").createNewFile();
            } catch (IOException e) {
                // ignored, I hate java
            }
        }
    }

    public static BlackMessagesController getInstance() {
        if (instance == null) {
            instance = new BlackMessagesController();
        }
        return instance;
    }

    public void onMessageEdited(BlackSavePreferences prefs, TLRPC.Message newMessage) {
        try {
            onMessageEditedInner(prefs, newMessage, false);
        } catch (Exception e) {
            Log.e("BlackStaR", "error onMessageEdited", e);
            FileLog.e("onMessageEdited", e);
        }
    }

    public void onMessageEditedForce(BlackSavePreferences prefs) {
        try {
            onMessageEditedInner(prefs, prefs.getMessage(), true);
        } catch (Exception e) {
            Log.e("BlackStaR", "error onMessageEditedForce", e);
            FileLog.e("onMessageEditedForce", e);
        }
    }

    private void onMessageEditedInner(BlackSavePreferences prefs, TLRPC.Message newMessage, boolean force) {
        if (!BlackConfig.saveEditedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        var oldMessage = prefs.getMessage();

        boolean sameMedia = oldMessage.media == newMessage.media ||
                (oldMessage.media != null && newMessage.media != null && oldMessage.media.getClass() == newMessage.media.getClass());
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
        }

        if (force) {
            sameMedia = false;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        var revision = new EditedMessage();
        BlackMessageUtils.map(prefs, revision);
        BlackMessageUtils.mapMedia(prefs, revision, !sameMedia);

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath)) {
            var lastRevision = editedMessageDao.getLastRevision(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId());

            if (lastRevision != null && !TextUtils.equals(revision.mediaPath, lastRevision.mediaPath) && lastRevision.mediaPath != null && !lastRevision.mediaPath.contains(attachmentsSubfolder)) {
                // update previous revisions to reflect media change
                // like, there's no previous file, so replace it with one we copied before...
                editedMessageDao.updateAttachmentForRevisionsBetweenDates(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId(), lastRevision.mediaPath, revision.mediaPath);
            }
        }

        editedMessageDao.insert(revision);

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(prefs.getAccountId()).postNotificationName(BlackConstants.MESSAGE_EDITED_NOTIFICATION, prefs.getDialogId(), prefs.getMessageId());
        });
    }

    public void onMessageDeleted(BlackSavePreferences prefs) {
        if (prefs.getMessage() == null) {
            Log.w("BlackStaR", "null msg ?");
            return;
        }

        try {
            onMessageDeletedInner(prefs);
        } catch (Exception e) {
            Log.e("BlackStaR", "error onMessageDeleted", e);
            FileLog.e("onMessageDeleted", e);
        }
    }

    private void onMessageDeletedInner(BlackSavePreferences prefs) {
        if (!BlackConfig.saveDeletedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        if (deletedMessageDao.exists(prefs.getUserId(), prefs.getDialogId(), prefs.getTopicId(), prefs.getMessageId())) {
            return;
        }

        var deletedMessage = new DeletedMessage();
        deletedMessage.userId = prefs.getUserId();
        deletedMessage.dialogId = prefs.getDialogId();
        deletedMessage.messageId = prefs.getMessageId();
        deletedMessage.entityCreateDate = prefs.getRequestCatchTime();

        var msg = prefs.getMessage();

        Log.d("BlackStaR", "saving message " + prefs.getMessageId() + " for " + prefs.getDialogId() + " with topic " + prefs.getTopicId());

        BlackMessageUtils.map(prefs, deletedMessage);
        BlackMessageUtils.mapMedia(prefs, deletedMessage, true);

        var fakeMsgId = deletedMessageDao.insert(deletedMessage);

        if (msg != null && msg.reactions != null && BlackConfig.saveReactions) {
            processDeletedReactions(fakeMsgId, msg.reactions);
        }
    }

    private void processDeletedReactions(long fakeMessageId, TLRPC.TL_messageReactions reactions) {
        for (var reaction : reactions.results) {
            if (reaction.reaction instanceof TLRPC.TL_reactionEmpty) {
                continue;
            }

            var deletedReaction = new DeletedMessageReaction();
            deletedReaction.deletedMessageId = fakeMessageId;
            deletedReaction.count = reaction.count;
            deletedReaction.selfSelected = reaction.chosen;

            if (reaction.reaction instanceof TLRPC.TL_reactionEmoji) {
                deletedReaction.emoticon = ((TLRPC.TL_reactionEmoji) reaction.reaction).emoticon;
            } else if (reaction.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                deletedReaction.documentId = ((TLRPC.TL_reactionCustomEmoji) reaction.reaction).document_id;
                deletedReaction.isCustom = true;
            } else {
                Log.e("BlackStaR", "fake news emoji");
                continue;
            }

            deletedMessageDao.insertReaction(deletedReaction);
        }
    }

    public boolean hasAnyRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.hasAnyRevisions(userId, dialogId, messageId);
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.getAllRevisions(userId, dialogId, messageId);
    }

    public DeletedMessageFull getMessage(long userId, long dialogId, int messageId) {
        return deletedMessageDao.getMessage(userId, dialogId, messageId);
    }

    public List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit) {
        return deletedMessageDao.getMessages(userId, dialogId, topicId, startId, endId, limit);
    }

    public List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId) {
        return deletedMessageDao.getMessagesGrouped(userId, dialogId, groupedId);
    }

    public void delete(long userId, long dialogId, int messageId) {
        var msg = getMessage(userId, dialogId, messageId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, messageId);

        if (!TextUtils.isEmpty(msg.message.mediaPath)) {
            var p = new File(msg.message.mediaPath);
            if (p.exists()) {
                try {
                    p.delete();
                } catch (Exception e) {
                    Log.e("BlackStaR", "failed to delete file " + msg.message.mediaPath, e);
                }
            }
        }
    }

    public void clean() {
        BlackData.clean();
        BlackData.create();

        // force to recreate a database to avoid crash
        instance = null;
    }
}
