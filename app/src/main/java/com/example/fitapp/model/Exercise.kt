package com.example.fitapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscle: String,
    val musclesPrimary: List<String> = emptyList(),
    val musclesSecondary: List<String> = emptyList(),
    val equipment: String,
    val equipmentList: List<String> = listOf(equipment),
    val category: String = "strength",
    val level: String = "beginner",
    val instructions: List<String> = emptyList(),
    val cues: List<String> = emptyList(),
    val imageAsset: String = "",
    val isPrimary: Boolean = true,
    val variantIds: List<String> = emptyList()
) {
    val displayAssetPath: String
        get() = AssetResolver.resolveDisplayAssetPath(imageAsset, muscle, name, musclesPrimary)
}
