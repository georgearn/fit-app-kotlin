package com.example.fitapp.generator

import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.model.Exercise
import com.example.fitapp.model.Workout
import com.example.fitapp.model.WorkoutExercise
import java.util.UUID

enum class ExerciseCountMode(val title: String) {
    TOTAL("Total Exercises"),
    PER_MUSCLE("Per Muscle Group")
}

object WorkoutGenerator {

    val EQUIPMENT_OPTIONS = listOf(
        "Barbell",
        "Dumbbell",
        "Bodyweight",
        "Band (handles)",
        "Band (plain)",
        "Cable",
        "Mat"
    )

    val PRESET_ROUTINES = mapOf(
        "Full Body" to listOf("Chest", "Back", "Legs", "Core"),
        "Upper Body" to listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps"),
        "Lower Body" to listOf("Legs", "Glutes", "Core"),
        "Push" to listOf("Chest", "Shoulders", "Triceps"),
        "Pull" to listOf("Back", "Biceps", "Forearms"),
        "Legs & Core" to listOf("Legs", "Glutes", "Core")
    )

    fun generateVariations(
        muscles: Set<String>,
        equipment: Set<String>,
        exerciseCount: Int = 5,
        countMode: ExerciseCountMode = ExerciseCountMode.PER_MUSCLE,
        perMuscleCounts: Map<String, Int> = emptyMap(),
        variationCount: Int = 3
    ): List<Workout> {
        val targetMuscles = if (muscles.isEmpty()) listOf("Chest", "Back", "Legs", "Core") else muscles.toList()

        // Filter candidate exercises
        val candidates = ExerciseCatalog.allExercises.filter { exercise ->
            val matchesMuscle = targetMuscles.any { it.equals(exercise.muscle, ignoreCase = true) }
            val matchesEquipment = equipment.isEmpty() ||
                    equipment.any { eq -> exercise.equipmentList.any { it.contains(eq, ignoreCase = true) } }
            matchesMuscle && matchesEquipment
        }

        if (candidates.isEmpty()) {
            return emptyList()
        }

        // Group by muscle
        val byMuscle = mutableMapOf<String, MutableList<Exercise>>()
        for (m in targetMuscles) {
            byMuscle[m] = candidates.filter { it.muscle.equals(m, ignoreCase = true) }.toMutableList()
        }

        val results = mutableListOf<Workout>()

        for (v in 0 until variationCount) {
            val picked = mutableListOf<Exercise>()
            val seenIds = mutableSetOf<String>()

            val muscleKeys = targetMuscles.filter { (byMuscle[it]?.size ?: 0) > 0 }
            if (muscleKeys.isEmpty()) continue

            if (countMode == ExerciseCountMode.PER_MUSCLE) {
                // Pick target count per each selected muscle group
                for (m in muscleKeys) {
                    val list = byMuscle[m] ?: continue
                    val targetCountForThisMuscle = perMuscleCounts[m] ?: exerciseCount
                    var pickedForThisMuscle = 0
                    var offset = 0
                    while (pickedForThisMuscle < targetCountForThisMuscle && offset < list.size) {
                        val ex = list[(v + offset) % list.size]
                        if (ex.id !in seenIds) {
                            seenIds.add(ex.id)
                            picked.add(ex)
                            pickedForThisMuscle++
                        }
                        offset++
                    }
                }
            } else {
                // Round-robin total count across selected muscles
                var attempts = 0
                while (picked.size < exerciseCount && attempts < 30) {
                    for (i in muscleKeys.indices) {
                        val mIndex = (i + v) % muscleKeys.size
                        val m = muscleKeys[mIndex]
                        val list = byMuscle[m] ?: continue
                        if (list.isNotEmpty()) {
                            val ex = list[(v + attempts) % list.size]
                            if (ex.id !in seenIds) {
                                seenIds.add(ex.id)
                                picked.add(ex)
                                if (picked.size >= exerciseCount) break
                            }
                        }
                    }
                    attempts++
                }
            }

            if (picked.isNotEmpty()) {
                val titleSuffix = when (v) {
                    0 -> "Strength & Power"
                    1 -> "Hypertrophy Focus"
                    else -> "Conditioning Circuit"
                }
                val workoutExercises = picked.mapIndexed { idx, ex ->
                    val sets = if (v == 0) 3 else 4
                    val reps = when {
                        ex.equipment == "Bodyweight" && ex.muscle == "Core" -> "30-45s"
                        ex.equipment == "Barbell" -> "6-8"
                        ex.equipment == "Dumbbell" -> "10-12"
                        else -> "12-15"
                    }
                    val rest = if (v == 0) 90 else 60
                    WorkoutExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        muscle = ex.muscle,
                        equipment = ex.equipment,
                        sets = sets,
                        reps = reps,
                        restSec = rest,
                        imageAsset = ex.imageAsset
                    )
                }

                val countDescription = if (countMode == ExerciseCountMode.PER_MUSCLE) {
                    "$exerciseCount exercises per muscle group (${picked.size} total)"
                } else {
                    "${picked.size} exercises total"
                }

                val workout = Workout(
                    id = UUID.randomUUID().toString(),
                    name = "Variation ${v + 1}: $titleSuffix",
                    note = "$countDescription across ${targetMuscles.joinToString(", ")} using ${if (equipment.isEmpty()) "Any Equipment" else equipment.joinToString(", ")}.",
                    exercises = workoutExercises,
                    targetMuscles = picked.map { it.muscle }.distinct()
                )
                results.add(workout)
            }
        }

        return results
    }
}
