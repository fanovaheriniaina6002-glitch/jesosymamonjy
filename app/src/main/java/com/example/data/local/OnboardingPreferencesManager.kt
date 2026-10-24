package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jesosy_mamonjy_onboarding_datastore"
)

data class OnboardingUserData(
    val name: String = "",
    val gender: String = "Lehilahy",
    val region: String = "Ambohipo foibe",
    val imageUri: String? = null,
    val isOnboardingCompleted: Boolean = false
)

class OnboardingPreferencesManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val sharedPrefs = appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    val onboardingDataFlow: Flow<OnboardingUserData> = appContext.onboardingDataStore.data.map { prefs ->
        val resolvedRegion = prefs[KEY_USER_REGION]
            ?: sharedPrefs.getString(PREF_USER_REGION, "Ambohipo foibe")
            ?: "Ambohipo foibe"
        _activeRegionFlow.value = resolvedRegion
        OnboardingUserData(
            name = prefs[KEY_USER_NAME] ?: sharedPrefs.getString(PREF_USER_NAME, "") ?: "",
            gender = prefs[KEY_USER_GENDER] ?: sharedPrefs.getString(PREF_USER_GENDER, "Lehilahy") ?: "Lehilahy",
            region = resolvedRegion,
            imageUri = (prefs[KEY_IMAGE_URI] ?: sharedPrefs.getString(PREF_IMAGE_URI, null))?.takeIf { it.isNotBlank() },
            isOnboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED]
                ?: sharedPrefs.getBoolean(PREF_ONBOARDING_COMPLETED, false)
        )
    }

    init {
        val initialRegion = sharedPrefs.getString(PREF_USER_REGION, null)
        if (!initialRegion.isNullOrBlank()) {
            _activeRegionFlow.value = initialRegion
        }
    }

    fun getRegionSync(): String {
        val saved = sharedPrefs.getString(PREF_USER_REGION, null)?.takeIf { it.isNotBlank() }
        if (saved != null) {
            _activeRegionFlow.value = saved
            return saved
        }
        return _activeRegionFlow.value
    }

    fun setRegionImmediate(region: String) {
        val cleanRegion = region.trim().ifEmpty { "Ambohipo foibe" }
        _activeRegionFlow.value = cleanRegion
        sharedPrefs.edit().putString(PREF_USER_REGION, cleanRegion).apply()
        notifyRegionListeners(cleanRegion)
    }

    fun isOnboardingCompletedSync(): Boolean {
        return sharedPrefs.getBoolean(PREF_ONBOARDING_COMPLETED, false)
    }

    suspend fun saveOnboardingProfile(
        name: String,
        gender: String,
        region: String,
        imageUri: String?,
        songDao: SongDao? = null
    ) {
        val cleanName = name.trim().ifEmpty {
            if (gender.equals("Vehivavy", ignoreCase = true) || gender.equals("female", ignoreCase = true)) {
                "Ranabavy Mpino"
            } else {
                "Rahalahy Mpino"
            }
        }
        val cleanGenderLabel = if (
            gender.equals("Vehivavy", ignoreCase = true) || gender.equals("female", ignoreCase = true)
        ) {
            "Vehivavy"
        } else {
            "Lehilahy"
        }
        val roomGenderCode = if (cleanGenderLabel == "Vehivavy") "female" else "male"
        val cleanRegion = region.trim().ifEmpty { "Ambohipo foibe" }
        val cleanUri = imageUri?.trim().orEmpty()

        // 0. Update in-memory StateFlow immediately for 0-latency observers
        _activeRegionFlow.value = cleanRegion
        notifyRegionListeners(cleanRegion)

        // 1. Save to SharedPreferences for 0-lag synchronous startup check
        sharedPrefs.edit()
            .putString(PREF_USER_NAME, cleanName)
            .putString(PREF_USER_GENDER, cleanGenderLabel)
            .putString(PREF_USER_REGION, cleanRegion)
            .putString(PREF_IMAGE_URI, cleanUri)
            .putBoolean(PREF_ONBOARDING_COMPLETED, true)
            .apply()

        // 2. Save to Jetpack DataStore Preferences
        appContext.onboardingDataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = cleanName
            prefs[KEY_USER_GENDER] = cleanGenderLabel
            prefs[KEY_USER_REGION] = cleanRegion
            prefs[KEY_IMAGE_URI] = cleanUri
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }

        // 3. Synchronize with local Room Database (UserProfileEntity)
        val dao = songDao ?: AppDatabase.getInstance(appContext).songDao()
        val existing = dao.getUserProfileOnce()
        val updatedEntity = (existing ?: UserProfileEntity()).copy(
            id = 1,
            displayName = cleanName,
            gender = roomGenderCode,
            region = cleanRegion,
            photoUri = cleanUri,
            isOnboardingCompleted = true
        )
        dao.saveUserProfile(updatedEntity)
    }

    companion object {
        private const val SHARED_PREFS_NAME = "jesosy_mamonjy_onboarding_prefs"
        private const val PREF_USER_NAME = "user_name"
        private const val PREF_USER_GENDER = "user_gender"
        private const val PREF_USER_REGION = "user_region"
        private const val PREF_IMAGE_URI = "profile_image_uri"
        private const val PREF_ONBOARDING_COMPLETED = "is_onboarding_completed"

        private val KEY_USER_NAME = stringPreferencesKey(PREF_USER_NAME)
        private val KEY_USER_GENDER = stringPreferencesKey(PREF_USER_GENDER)
        private val KEY_USER_REGION = stringPreferencesKey(PREF_USER_REGION)
        private val KEY_IMAGE_URI = stringPreferencesKey(PREF_IMAGE_URI)
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey(PREF_ONBOARDING_COMPLETED)

        private val _activeRegionFlow = kotlinx.coroutines.flow.MutableStateFlow("Ambohipo foibe")
        val activeRegionFlow: kotlinx.coroutines.flow.StateFlow<String> = _activeRegionFlow

        private val regionListeners = java.util.concurrent.CopyOnWriteArrayList<(String) -> Unit>()

        fun registerRegionListener(listener: (String) -> Unit) {
            if (!regionListeners.contains(listener)) {
                regionListeners.add(listener)
            }
        }

        fun unregisterRegionListener(listener: (String) -> Unit) {
            regionListeners.remove(listener)
        }

        private fun notifyRegionListeners(region: String) {
            for (listener in regionListeners) {
                listener(region)
            }
        }
    }
}
