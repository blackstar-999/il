/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.blackstar.blackstar.database.dao.DeletedMessageDao;
import com.blackstar.blackstar.database.dao.EditedMessageDao;
import com.blackstar.blackstar.database.entities.DeletedMessage;
import com.blackstar.blackstar.database.entities.DeletedMessageReaction;
import com.blackstar.blackstar.database.entities.EditedMessage;

@Database(entities = {
        EditedMessage.class,
        DeletedMessage.class,
        DeletedMessageReaction.class
}, version = 21)
public abstract class BlackDatabase extends RoomDatabase {
    public abstract EditedMessageDao editedMessageDao();

    public abstract DeletedMessageDao deletedMessageDao();
}