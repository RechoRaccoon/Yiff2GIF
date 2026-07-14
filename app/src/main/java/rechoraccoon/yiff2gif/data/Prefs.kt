package rechoraccoon.yiff2gif.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "yiff2gif_prefs")

object PrefKeys {
    val USERNAME = stringPreferencesKey("username")
    val API_KEY = stringPreferencesKey("api_key")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
}

class Prefs(private val context: Context) {

    suspend fun saveCredentials(username: String, apiKey: String) {
        context.dataStore.edit {
            it[PrefKeys.USERNAME] = username
            it[PrefKeys.API_KEY] = apiKey
        }
    }

    suspend fun getCredentials(): Pair<String, String>? {
        val prefs = context.dataStore.data.first()
        val u = prefs[PrefKeys.USERNAME]
        val k = prefs[PrefKeys.API_KEY]
        return if (!u.isNullOrBlank() && !k.isNullOrBlank()) u to k else null
    }

    suspend fun clearCredentials() {
        context.dataStore.edit {
            it.remove(PrefKeys.USERNAME)
            it.remove(PrefKeys.API_KEY)
        }
    }

    suspend fun getGridColumns(): Int {
        return context.dataStore.data.first()[PrefKeys.GRID_COLUMNS] ?: 3
    }

    suspend fun saveGridColumns(columns: Int) {
        context.dataStore.edit {
            it[PrefKeys.GRID_COLUMNS] = columns
        }
    }
}
