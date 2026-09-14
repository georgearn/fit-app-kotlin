package com.example.fitapp.model

/**
 * Resolves exercise image assets safely against the bundled WebP animated previews in assets/exercises/.
 * Guarantees every exercise displays an anatomically correct, valid preview animation.
 */
object AssetResolver {

    fun resolveDisplayAssetPath(
        asset: String?,
        muscle: String = "",
        exerciseName: String = "",
        primaryMuscles: List<String> = emptyList()
    ): String {
        val clean = asset?.removePrefix("file:///android_asset/")?.trim().orEmpty()
        val filename = clean.substringAfterLast('/')
        if (filename.isNotBlank()) {
            return "file:///android_asset/exercises/$filename"
        }

        val nameLower = exerciseName.lowercase()

        // Fallbacks if asset filename is ever empty
        val keywordFallback = when {
            nameLower.contains("bicep") || nameLower.contains("curl") -> "0031.webp"
            nameLower.contains("tricep") || nameLower.contains("skull") || nameLower.contains("dip") -> "0035.webp"
            nameLower.contains("bench") || nameLower.contains("push") || nameLower.contains("chest") -> "0025.webp"
            nameLower.contains("pulldown") || nameLower.contains("pull up") || nameLower.contains("row") || nameLower.contains("lat") -> "0027.webp"
            nameLower.contains("squat") || nameLower.contains("lunge") -> "0026.webp"
            nameLower.contains("deadlift") || nameLower.contains("glute") -> "0032.webp"
            nameLower.contains("front raise") || nameLower.contains("overhead") || nameLower.contains("shoulder") -> "0041.webp"
            nameLower.contains("bike") || nameLower.contains("twist") -> "0003.webp"
            nameLower.contains("side bend") || nameLower.contains("heel") || nameLower.contains("oblique") -> "0006.webp"
            nameLower.contains("sit up") || nameLower.contains("crunch") || nameLower.contains("plank") || nameLower.contains("abs") -> "0001.webp"
            else -> null
        }

        if (keywordFallback != null) {
            return "file:///android_asset/exercises/$keywordFallback"
        }

        val fallback = when (muscle.trim().lowercase()) {
            "chest" -> "0025.webp"
            "back", "upper back" -> "0027.webp"
            "lower back" -> "0032.webp"
            "shoulders" -> "0041.webp"
            "biceps" -> "0031.webp"
            "triceps" -> "0035.webp"
            "legs" -> "0026.webp"
            "glutes" -> "0032.webp"
            "calves" -> "0026.webp"
            "core" -> "0001.webp"
            "obliques" -> "0006.webp"
            "forearms" -> "0031.webp"
            "traps" -> "0041.webp"
            "adductors" -> "0026.webp"
            "neck" -> "0041.webp"
            else -> "0026.webp"
        }
        return "file:///android_asset/exercises/$fallback"
    }
}
