package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.util.Calendar

internal fun signInReminderDay(now: Calendar): Int? {
    if (now.get(Calendar.HOUR_OF_DAY) < 1) return null
    return now.get(Calendar.YEAR) * 10000 +
        (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH)
}

internal class SignInReminderStore(private val store: DataStore<Preferences>) {
    suspend fun claim(accountUid: String, day: Int): Boolean {
        val key = intPreferencesKey("sign_in_reminder_day_$accountUid")
        var claimed = false
        store.edit { preferences ->
            if (preferences[key] != day) {
                preferences[key] = day
                claimed = true
            }
        }
        return claimed
    }
}
