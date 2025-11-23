package io.github.redballoonx.fableverse.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

//Saves the User-Settings
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fableverse_settings", Context.MODE_PRIVATE)

    fun saveAudiobookFolderUri(uri: String) {
        prefs.edit { putString(KEY_AUDIOBOOK_FOLDER, uri) }
    }

    fun getAudiobookFolderUri(): String? {
        return prefs.getString(KEY_AUDIOBOOK_FOLDER, null)
    }

    companion object {
        private const val KEY_AUDIOBOOK_FOLDER = "audiobook_folder_uri"
    }
}