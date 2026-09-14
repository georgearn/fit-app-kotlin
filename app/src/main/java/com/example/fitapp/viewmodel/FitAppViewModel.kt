package com.example.fitapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.fitapp.data.BodyMapping
import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.data.WorkoutRepository
import com.example.fitapp.generator.AiConnectionMode
import com.example.fitapp.model.Exercise
import com.example.fitapp.model.Workout
import com.example.fitapp.model.WorkoutExercise
import com.example.fitapp.ui.components.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FitAppViewModel(application: Application) : AndroidViewModel(application) {

    init {
        ExerciseCatalog.initialize(application.applicationContext)
        BodyMapping.initialize(application.applicationContext)
    }

    private val prefs = application.getSharedPreferences("fitapp_settings", Context.MODE_PRIVATE)

    private val repository = WorkoutRepository(application)

    val workouts: StateFlow<List<Workout>> = repository.workouts

    private val _selectedTab = MutableStateFlow(AppTab.BODY)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("pref_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _useMaterialYou = MutableStateFlow(prefs.getBoolean("pref_material_you", false))
    val useMaterialYou: StateFlow<Boolean> = _useMaterialYou.asStateFlow()

    private val _accentColorKey = MutableStateFlow(prefs.getString("pref_accent_key", "CYAN") ?: "CYAN")
    val accentColorKey: StateFlow<String> = _accentColorKey.asStateFlow()

    private val _aiConnectionMode = MutableStateFlow(
        try {
            val savedMode = prefs.getString("pref_ai_mode", null)
            if (savedMode != null) AiConnectionMode.valueOf(savedMode) else AiConnectionMode.CLOUD_GEMINI
        } catch (e: Exception) {
            AiConnectionMode.CLOUD_GEMINI
        }
    )
    val aiConnectionMode: StateFlow<AiConnectionMode> = _aiConnectionMode.asStateFlow()

    private val _userApiKey = MutableStateFlow(prefs.getString("pref_user_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _exerciseToAddToWorkout = MutableStateFlow<Exercise?>(null)
    val exerciseToAddToWorkout: StateFlow<Exercise?> = _exerciseToAddToWorkout.asStateFlow()

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        prefs.edit().putBoolean("pref_dark_theme", next).apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("pref_dark_theme", isDark).apply()
    }

    fun setUseMaterialYou(enabled: Boolean) {
        _useMaterialYou.value = enabled
        prefs.edit().putBoolean("pref_material_you", enabled).apply()
    }

    fun setAccentColorKey(key: String) {
        _accentColorKey.value = key
        prefs.edit().putString("pref_accent_key", key).apply()
    }

    fun setAiConnectionMode(mode: AiConnectionMode) {
        _aiConnectionMode.value = mode
        prefs.edit().putString("pref_ai_mode", mode.name).apply()
    }

    fun setUserApiKey(key: String) {
        _userApiKey.value = key.trim()
        prefs.edit().putString("pref_user_api_key", key.trim()).apply()
    }

    fun clearUserApiKey() {
        _userApiKey.value = ""
        prefs.edit().remove("pref_user_api_key").apply()
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun startWorkout(workout: Workout) {
        _activeWorkout.value = workout
    }

    fun finishActiveWorkout() {
        _activeWorkout.value = null
    }

    fun saveWorkout(workout: Workout) {
        repository.saveWorkout(workout)
    }

    fun deleteWorkout(workoutId: String) {
        repository.deleteWorkout(workoutId)
    }

    fun toggleFavorite(workoutId: String) {
        repository.toggleFavorite(workoutId)
    }

    fun setExerciseToAddToWorkout(exercise: Exercise?) {
        _exerciseToAddToWorkout.value = exercise
    }

    fun addExerciseToWorkout(workoutId: String, exercise: Exercise) {
        val workoutEx = WorkoutExercise(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            muscle = exercise.muscle,
            equipment = exercise.equipment,
            sets = 3,
            reps = "10-12",
            restSec = 60,
            imageAsset = exercise.imageAsset
        )
        repository.addExerciseToWorkout(workoutId, workoutEx)
        _exerciseToAddToWorkout.value = null
    }
}
