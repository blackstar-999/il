/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class DeletedMessage extends BlackMessageBase {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;
}
