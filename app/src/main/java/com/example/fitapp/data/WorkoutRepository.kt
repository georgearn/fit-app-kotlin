package com.example.fitapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.fitapp.model.Workout
import com.example.fitapp.model.WorkoutExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WorkoutRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitapp_workouts_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    init {
        loadWorkouts()
    }

    private fun loadWorkouts() {
        val storedJson = prefs.getString("saved_workouts_list", null)
        if (storedJson.isNullOrBlank()) {
            _workouts.value = emptyList()
        } else {
            try {
                val list = json.decodeFromString<List<Workout>>(storedJson)
                // Filter out any preset workouts as requested by user
                val nonPresets = list.filterNot { it.isPreset }
                _workouts.value = nonPresets
                if (nonPresets.size != list.size) {
                    saveToDisk(nonPresets)
                }
            } catch (e: Exception) {
                _workouts.value = emptyList()
            }
        }
    }

    private fun saveToDisk(list: List<Workout>) {
        try {
            val str = json.encodeToString(list)
            prefs.edit().putString("saved_workouts_list", str).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveWorkout(workout: Workout) {
        val current = _workouts.value.toMutableList()
        val index = current.indexOfFirst { it.id == workout.id }
        if (index >= 0) {
            current[index] = workout
        } else {
            current.add(0, workout)
        }
        _workouts.value = current
        saveToDisk(current)
    }

    fun deleteWorkout(workoutId: String) {
        val updated = _workouts.value.filterNot { it.id == workoutId }
        _workouts.value = updated
        saveToDisk(updated)
    }

    fun toggleFavorite(workoutId: String) {
        val updated = _workouts.value.map {
            if (it.id == workoutId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _workouts.value = updated
        saveToDisk(updated)
    }

    fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise) {
        val current = _workouts.value.toMutableList()
        val index = current.indexOfFirst { it.id == workoutId }
        if (index >= 0) {
            val target = current[index]
            val updatedExercises = target.exercises + exercise
            val updatedMuscles = (target.targetMuscles + exercise.muscle).distinct()
            current[index] = target.copy(exercises = updatedExercises, targetMuscles = updatedMuscles)
            _workouts.value = current
            saveToDisk(current)
        }
    }

    private fun defaultPresets(): List<Workout> {
        return listOf(
            Workout(
                id = "preset_full_body",
                name = "Full Body Power Blast",
                note = "Balanced compound movements hitting chest, back, legs, and core in ~40 mins.",
                isPreset = true,
                isFavorite = true,
                targetMuscles = listOf("Chest", "Back", "Legs", "Core"),
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "barbell_bench_press",
                        exerciseName = "Barbell Bench Press",
                        muscle = "Chest",
                        equipment = "Barbell",
                        sets = 3,
                        reps = "8-10",
                        restSec = 90,
                        imageAsset = "exercises/0025.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "barbell_row",
                        exerciseName = "Barbell Bent-Over Row",
                        muscle = "Back",
                        equipment = "Barbell",
                        sets = 3,
                        reps = "8-10",
                        restSec = 90,
                        imageAsset = "exercises/0027.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "barbell_squat",
                        exerciseName = "Barbell Back Squat",
                        muscle = "Legs",
                        equipment = "Barbell",
                        sets = 3,
                        reps = "6-8",
                        restSec = 120,
                        imageAsset = "exercises/0026.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "plank",
                        exerciseName = "Forearm Plank",
                        muscle = "Core",
                        equipment = "Bodyweight",
                        sets = 3,
                        reps = "45s",
                        restSec = 60,
                        imageAsset = "exercises/0001.webp"
                    )
                )
            ),
            Workout(
                id = "preset_upper_hypertrophy",
                name = "Upper Body Hypertrophy",
                note = "High-energy push & pull session for chest, lats, shoulders, and arms.",
                isPreset = true,
                targetMuscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps"),
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "incline_dumbbell_press",
                        exerciseName = "Incline Dumbbell Press",
                        muscle = "Chest",
                        equipment = "Dumbbell",
                        sets = 4,
                        reps = "10-12",
                        restSec = 75,
                        imageAsset = "exercises/0031.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "lat_pulldown",
                        exerciseName = "Lat Pulldown",
                        muscle = "Back",
                        equipment = "Cable",
                        sets = 4,
                        reps = "10-12",
                        restSec = 75,
                        imageAsset = "exercises/0037.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "dumbbell_lateral_raise",
                        exerciseName = "Dumbbell Lateral Raise",
                        muscle = "Shoulders",
                        equipment = "Dumbbell",
                        sets = 3,
                        reps = "12-15",
                        restSec = 60,
                        imageAsset = "exercises/0032.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "barbell_curl",
                        exerciseName = "Barbell Bicep Curl",
                        muscle = "Biceps",
                        equipment = "Barbell",
                        sets = 3,
                        reps = "10-12",
                        restSec = 60,
                        imageAsset = "exercises/0030.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "triceps_rope_pushdown",
                        exerciseName = "Triceps Rope Pushdown",
                        muscle = "Triceps",
                        equipment = "Cable",
                        sets = 3,
                        reps = "12-15",
                        restSec = 60,
                        imageAsset = "exercises/0035.webp"
                    )
                )
            ),
            Workout(
                id = "preset_lower_glute",
                name = "Legs & Glutes Crusher",
                note = "Target quads, hamstrings, and glutes with heavy thrusts and lunges.",
                isPreset = true,
                targetMuscles = listOf("Legs", "Glutes", "Core"),
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "barbell_hip_thrust",
                        exerciseName = "Barbell Hip Thrust",
                        muscle = "Glutes",
                        equipment = "Barbell",
                        sets = 4,
                        reps = "10-12",
                        restSec = 90,
                        imageAsset = "exercises/0023.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "romanian_deadlift",
                        exerciseName = "Barbell Romanian Deadlift",
                        muscle = "Legs",
                        equipment = "Barbell",
                        sets = 3,
                        reps = "8-10",
                        restSec = 90,
                        imageAsset = "exercises/0023.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "walking_lunges",
                        exerciseName = "Dumbbell Walking Lunges",
                        muscle = "Legs",
                        equipment = "Dumbbell",
                        sets = 3,
                        reps = "12 each",
                        restSec = 60,
                        imageAsset = "exercises/0026.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "hanging_leg_raise",
                        exerciseName = "Hanging Leg Raise",
                        muscle = "Core",
                        equipment = "Bodyweight",
                        sets = 3,
                        reps = "12-15",
                        restSec = 60,
                        imageAsset = "exercises/0034.webp"
                    )
                )
            ),
            Workout(
                id = "preset_home_dumbbell",
                name = "Home Dumbbell & Mat Express",
                note = "Zero gym machines required. Complete full-body pump in 25 minutes.",
                isPreset = true,
                targetMuscles = listOf("Chest", "Back", "Legs", "Core"),
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "push_ups",
                        exerciseName = "Push-ups",
                        muscle = "Chest",
                        equipment = "Bodyweight",
                        sets = 3,
                        reps = "15-20",
                        restSec = 60,
                        imageAsset = "exercises/0031.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "dumbbell_one_arm_row",
                        exerciseName = "One-Arm Dumbbell Row",
                        muscle = "Back",
                        equipment = "Dumbbell",
                        sets = 3,
                        reps = "10-12",
                        restSec = 60,
                        imageAsset = "exercises/0027.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "goblet_squat",
                        exerciseName = "Dumbbell Goblet Squat",
                        muscle = "Legs",
                        equipment = "Dumbbell",
                        sets = 3,
                        reps = "12-15",
                        restSec = 60,
                        imageAsset = "exercises/0026.webp"
                    ),
                    WorkoutExercise(
                        exerciseId = "bicycle_crunches",
                        exerciseName = "Bicycle Crunches",
                        muscle = "Core",
                        equipment = "Mat",
                        sets = 3,
                        reps = "20 total",
                        restSec = 45,
                        imageAsset = "exercises/0001.webp"
                    )
                )
            )
        )
    }
}
