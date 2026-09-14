package com.example.fitapp.data

import android.content.Context
import com.example.fitapp.model.Exercise
import kotlinx.serialization.json.Json

object ExerciseCatalog {

    private val json = Json { ignoreUnknownKeys = true }
    private var loadedExercises: List<Exercise>? = null

    // Fast lookup maps
    private var byIdMap: Map<String, Exercise> = emptyMap()
    private var byMuscleMap: Map<String, List<Exercise>> = emptyMap()

    fun initialize(context: Context) {
        if (loadedExercises != null) return
        synchronized(this) {
            if (loadedExercises != null) return
            try {
                val jsonStr = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<List<Exercise>>(jsonStr)
                loadedExercises = parsed
                byIdMap = parsed.associateBy { it.id }
                byMuscleMap = parsed.groupBy { it.muscle.lowercase() }
            } catch (e: Exception) {
                e.printStackTrace()
                loadedExercises = fallbackExercises
                byIdMap = fallbackExercises.associateBy { it.id }
                byMuscleMap = fallbackExercises.groupBy { it.muscle.lowercase() }
            }
        }
    }

    val allExercises: List<Exercise>
        get() = loadedExercises ?: fallbackExercises

    fun getByMuscle(muscle: String): List<Exercise> {
        val m = muscle.trim().lowercase()
        return allExercises.filter { ex ->
            val exMuscle = ex.muscle.lowercase()
            exMuscle == m ||
            ex.musclesPrimary.any { it.lowercase().contains(m) } ||
            (m == "legs" && (exMuscle in listOf("legs", "quadriceps", "hamstrings", "quads", "hamstring"))) ||
            (m == "calves" && (exMuscle in listOf("calves", "calf"))) ||
            (m == "adductors" && (exMuscle in listOf("adductors", "adductor", "groin"))) ||
            (m == "glutes" && (exMuscle in listOf("glutes", "gluteal", "glute"))) ||
            (m == "upper back" && (exMuscle in listOf("upper back", "lats"))) ||
            (m == "lower back" && (exMuscle in listOf("lower back", "erectors"))) ||
            (m == "traps" && (exMuscle in listOf("traps", "trapezius"))) ||
            (m == "back" && (exMuscle in listOf("back", "upper back", "lower back", "traps", "lats"))) ||
            (m == "core" && (exMuscle in listOf("core", "abs", "abdominals"))) ||
            (m == "obliques" && (exMuscle in listOf("obliques", "oblique"))) ||
            (m == "shoulders" && (exMuscle in listOf("shoulders", "deltoids", "delts"))) ||
            (m == "biceps" && (exMuscle in listOf("biceps", "bicep"))) ||
            (m == "triceps" && (exMuscle in listOf("triceps", "tricep"))) ||
            (m == "forearms" && (exMuscle in listOf("forearms", "forearm"))) ||
            (m == "chest" && (exMuscle in listOf("chest", "pectorals", "pecs"))) ||
            (m == "neck" && (exMuscle in listOf("neck")))
        }
    }

    fun getById(id: String): Exercise? {
        return byIdMap[id] ?: allExercises.firstOrNull { it.id == id }
    }

    fun getVariants(exercise: Exercise): List<Exercise> {
        if (exercise.variantIds.isEmpty()) return emptyList()
        return exercise.variantIds.mapNotNull { getById(it) }
    }

    fun search(query: String): List<Exercise> {
        if (query.isBlank()) return allExercises
        val q = query.trim().lowercase()
        return allExercises.filter {
            it.name.lowercase().contains(q) ||
            it.muscle.lowercase().contains(q) ||
            it.equipment.lowercase().contains(q) ||
            it.musclesPrimary.any { m -> m.lowercase().contains(q) } ||
            it.musclesSecondary.any { m -> m.lowercase().contains(q) }
        }
    }

    private val fallbackExercises: List<Exercise> = listOf(
        Exercise(
            id = "0025",
            name = "Barbell Bench Press",
            muscle = "Chest",
            musclesPrimary = listOf("Chest"),
            musclesSecondary = listOf("Triceps", "Shoulders"),
            equipment = "Barbell",
            equipmentList = listOf("Barbell"),
            category = "strength",
            level = "intermediate",
            instructions = listOf(
                "Lie back on a flat bench. Grip the barbell with hands slightly wider than shoulder-width.",
                "Unrack the bar and hold it with arms straight over your chest.",
                "Inhale and lower the barbell slowly until it lightly touches your mid-chest.",
                "Push the bar back up explosively to starting position while exhaling."
            ),
            cues = listOf("Keep shoulder blades retracted", "Plant feet flat on the floor"),
            imageAsset = "exercises/0025.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0027",
            name = "Barbell Bent Over Row",
            muscle = "Upper Back",
            musclesPrimary = listOf("Upper Back", "Lats"),
            musclesSecondary = listOf("Biceps", "Lower Back"),
            equipment = "Barbell",
            equipmentList = listOf("Barbell"),
            category = "strength",
            level = "intermediate",
            instructions = listOf(
                "Hinge forward at hips with knees slightly bent and spine neutral.",
                "Pull barbell up towards lower ribcage, driving elbows past torso.",
                "Squeeze lats and rhomboids at the top, then lower with control."
            ),
            cues = listOf("Keep spine flat and rigid", "Pull through elbows"),
            imageAsset = "exercises/0027.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0032",
            name = "Overhead Barbell Shoulder Press",
            muscle = "Shoulders",
            musclesPrimary = listOf("Shoulders"),
            musclesSecondary = listOf("Triceps", "Upper Chest"),
            equipment = "Barbell",
            equipmentList = listOf("Barbell"),
            category = "strength",
            level = "intermediate",
            instructions = listOf(
                "Rest barbell on front delts with hands shoulder-width apart.",
                "Press the bar overhead until arms are locked out.",
                "Lower under control back to collarbone level."
            ),
            cues = listOf("Squeeze glutes and brace core", "Push head through window at lockout"),
            imageAsset = "exercises/0032.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0026",
            name = "Barbell Back Squat",
            muscle = "Legs",
            musclesPrimary = listOf("Legs", "Quads"),
            musclesSecondary = listOf("Glutes", "Hamstrings", "Core"),
            equipment = "Barbell",
            equipmentList = listOf("Barbell"),
            category = "strength",
            level = "intermediate",
            instructions = listOf(
                "Position barbell across upper traps, stance shoulder-width apart.",
                "Descend by pushing hips back and breaking at knees until thighs reach parallel.",
                "Drive through mid-foot to stand back up tall."
            ),
            cues = listOf("Keep chest up and knees tracking over toes", "Brace core tightly"),
            imageAsset = "exercises/0026.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0007",
            name = "Barbell Bicep Curl",
            muscle = "Biceps",
            musclesPrimary = listOf("Biceps"),
            musclesSecondary = listOf("Forearms"),
            equipment = "Barbell",
            equipmentList = listOf("Barbell"),
            category = "strength",
            level = "beginner",
            instructions = listOf(
                "Stand tall holding barbell with underhand shoulder-width grip.",
                "Curl bar upwards toward chest while keeping elbows pinned to sides.",
                "Squeeze biceps at the top, then lower under control."
            ),
            cues = listOf("Prevent swinging your torso", "Full extension at the bottom"),
            imageAsset = "exercises/0007.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0035",
            name = "Triceps Cable Pushdown",
            muscle = "Triceps",
            musclesPrimary = listOf("Triceps"),
            musclesSecondary = listOf("Forearms"),
            equipment = "Cable",
            equipmentList = listOf("Cable"),
            category = "strength",
            level = "beginner",
            instructions = listOf(
                "Stand facing high pulley with rope or bar attachment.",
                "Push attachment down by extending elbows until arms are straight.",
                "Squeeze triceps at lockout, then return slowly to 90 degrees."
            ),
            cues = listOf("Keep upper arms pinned against ribs", "Control the negative phase"),
            imageAsset = "exercises/0035.webp",
            isPrimary = true
        ),
        Exercise(
            id = "0001",
            name = "3/4 Sit-up",
            muscle = "Core",
            musclesPrimary = listOf("Core", "Abs"),
            musclesSecondary = listOf("Hip Flexors"),
            equipment = "Bodyweight",
            equipmentList = listOf("Bodyweight"),
            category = "strength",
            level = "beginner",
            instructions = listOf(
                "Lie on your back with knees bent and feet flat on floor.",
                "Curl shoulders and torso three-quarters of the way up toward knees.",
                "Hold contraction briefly, then lower slowly."
            ),
            cues = listOf("Exhale on the crunch up", "Do not pull on your neck"),
            imageAsset = "exercises/0001.webp",
            isPrimary = true
        )
    )
}
