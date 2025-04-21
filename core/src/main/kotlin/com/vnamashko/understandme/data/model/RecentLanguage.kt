package com.vnamashko.understandme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vnamashko.understandme.translation.model.Language

@Entity
data class RecentLanguage(
    @PrimaryKey val code: String,
    val displayName: String
)

fun Language.toRecentLanguage() = RecentLanguage(code, displayName)