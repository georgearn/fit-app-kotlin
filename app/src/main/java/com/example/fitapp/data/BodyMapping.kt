package com.example.fitapp.data

import android.content.Context
import org.json.JSONObject

object BodyMapping {

    data class HitGrid(
        val gw: Int,
        val gh: Int,
        val x0: Float,
        val y0: Float,
        val s: Float,
        val labels: List<String>,
        val rows: List<String>
    )

    private var frontGrid: HitGrid? = null
    private var backGrid: HitGrid? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                val jsonStr = context.assets.open("body/body_data.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val hitObj = root.getJSONObject("hit")
                frontGrid = parseGrid(hitObj.getJSONObject("front"))
                backGrid = parseGrid(hitObj.getJSONObject("back"))
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseGrid(obj: JSONObject): HitGrid {
        val gw = obj.getInt("gw")
        val gh = obj.getInt("gh")
        val x0 = obj.getDouble("x0").toFloat()
        val y0 = obj.getDouble("y0").toFloat()
        val s = obj.getDouble("s").toFloat()
        val labelsArr = obj.getJSONArray("labels")
        val labels = (0 until labelsArr.length()).map { labelsArr.getString(it) }
        val rowsArr = obj.getJSONArray("rows")
        val rows = (0 until rowsArr.length()).map { rowsArr.getString(it) }
        return HitGrid(gw, gh, x0, y0, s, labels, rows)
    }

    /**
     * Standardizes muscle group names to the 15 anatomical muscle groups
     */
    fun mapToAppMuscleGroup(rawLabel: String): String {
        val trimmed = rawLabel.trim()
        val lower = trimmed.lowercase()
        return when (lower) {
            "chest" -> "Chest"
            "biceps" -> "Biceps"
            "triceps" -> "Triceps"
            "shoulders", "deltoids" -> "Shoulders"
            "forearms", "forearm" -> "Forearms"
            "core", "abs" -> "Core"
            "obliques" -> "Obliques"
            "glutes", "gluteal" -> "Glutes"
            "calves", "calf" -> "Calves"
            "adductors", "adductor", "groin" -> "Adductors"
            "legs", "quadriceps", "hamstrings", "quads", "hamstring", "thighs", "thigh" -> "Legs"
            "upper back", "lats", "rhomboids" -> "Upper Back"
            "lower back", "erectors" -> "Lower Back"
            "traps", "trapezius" -> "Traps"
            "neck" -> "Neck"
            "back" -> "Upper Back"
            else -> trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Given touch coordinates normalized inside the rendered SVG viewport (724 x 1448 aspect),
     * returns the muscle group tapped based on the exact hit-grid mapped against the body SVG.
     */
    fun hitTest(isFront: Boolean, svgX: Float, svgY: Float): String? {
        val grid = (if (isFront) frontGrid else backGrid) ?: return null
        val gx = ((svgX - grid.x0) / grid.s).toInt()
        val gy = ((svgY - grid.y0) / grid.s).toInt()

        // 1. Exact cell hit
        if (gy in 0 until grid.gh && gx in 0 until grid.gw) {
            val c = grid.rows[gy][gx]
            if (c != '.') {
                val labelIdx = c.digitToIntOrNull(36) ?: -1
                if (labelIdx in grid.labels.indices) {
                    return mapToAppMuscleGroup(grid.labels[labelIdx])
                }
            }
        }

        // 2. Nearest neighbor snap (radius up to 3 cells = 24px in SVG viewBox coordinates)
        var bestDist = Float.MAX_VALUE
        var bestLabel: String? = null
        for (dy in -3..3) {
            for (dx in -3..3) {
                val nx = gx + dx
                val ny = gy + dy
                if (ny in 0 until grid.gh && nx in 0 until grid.gw) {
                    val c = grid.rows[ny][nx]
                    if (c != '.') {
                        val labelIdx = c.digitToIntOrNull(36) ?: -1
                        if (labelIdx in grid.labels.indices) {
                            val dist = (dx * dx + dy * dy).toFloat()
                            if (dist < bestDist) {
                                bestDist = dist
                                bestLabel = grid.labels[labelIdx]
                            }
                        }
                    }
                }
            }
        }

        return bestLabel?.let { mapToAppMuscleGroup(it) }
    }

    /**
     * Resolves the SVG asset path based on orientation, highlighted muscle group, and theme.
     * Dark SVGs reside under assets/body/ while light SVGs reside under assets/body/light/.
     */
    fun getSvgAssetPath(isFront: Boolean, selectedMuscle: String?, isDark: Boolean = true): String {
        val folder = if (isDark) "body" else "body/light"
        val view = if (isFront) "front" else "back"
        if (selectedMuscle.isNullOrBlank()) {
            return "$folder/$view.svg"
        }
        val mapped = mapToAppMuscleGroup(selectedMuscle)
        val slug = mapped.lowercase().replace(' ', '_')
        return "$folder/${view}_${slug}.svg"
    }
}
