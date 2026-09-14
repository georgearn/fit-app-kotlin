package com.example.fitapp.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WorkoutExercise(
    val exerciseId: String,
    val exerciseName: String,
    val muscle: String,
    val equipment: String,
    val sets: Int = 3,
    val reps: String = "8-12",
    val restSec: Int = 60,
    val imageAsset: String = ""
) {
    val displayAssetPath: String
        get() = AssetResolver.resolveDisplayAssetPath(imageAsset, muscle, exerciseName)
}

@Serializable
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val note: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPreset: Boolean = false,
    val isFavorite: Boolean = false
) {
    val totalSets: Int
        get() = exercises.sumOf { it.sets }

    val estimatedMinutes: Int
        get() {
            // Est: ~45s per set + restSec
            val totalSeconds = exercises.sumOf { it.sets * (45 + it.restSec) }
            return ((totalSeconds + 59) / 60).coerceAtLeast(10)
        }
}
