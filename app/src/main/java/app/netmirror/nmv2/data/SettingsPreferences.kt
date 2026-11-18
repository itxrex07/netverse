package app.netmirror.nmv2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netmirror_settings")

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val PREFERRED_HZ = intPreferencesKey("nm_pref_hz")
        val HD_ENABLED = booleanPreferencesKey("nm_hd_enabled")
        val LAST_MIRROR_URL = stringPreferencesKey("nm_last_mirror_url")
    }

    val preferredRefreshRateHz: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREFERRED_HZ] ?: 0
    }

    val isHdEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HD_ENABLED] ?: false
    }

    val lastMirrorUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_MIRROR_URL]
    }

    suspend fun setPreferredRefreshRateHz(hz: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_HZ] = hz
        }
    }

    suspend fun setHdEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HD_ENABLED] = enabled
        }
    }

    suspend fun setLastMirrorUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_MIRROR_URL] = url
        }
    }
}
