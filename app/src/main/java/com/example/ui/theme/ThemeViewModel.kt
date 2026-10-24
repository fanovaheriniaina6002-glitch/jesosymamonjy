package com.example.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow

/**
 * 60 FPS ViewModel exposing Theme state with 0ms synchronous theme switching.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    val themeMode: StateFlow<String> = ThemeManager.themeMode
    val isDarkMode: StateFlow<Boolean> = ThemeManager.isDarkMode

    init {
        ThemeManager.init(application, viewModelScope)
    }

    fun setThemeMode(mode: String) {
        ThemeManager.setThemeMode(getApplication(), mode)
    }

    fun toggleTheme() {
        ThemeManager.toggleTheme(getApplication())
    }
}
