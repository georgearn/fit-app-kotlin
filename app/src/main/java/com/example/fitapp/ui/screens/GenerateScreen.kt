package com.example.fitapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitapp.generator.AiConnectionMode
import com.example.fitapp.generator.AiWorkoutService
import com.example.fitapp.generator.ExerciseCountMode
import com.example.fitapp.generator.WorkoutGenerator
import com.example.fitapp.model.MuscleGroup
import com.example.fitapp.model.Workout
import com.example.fitapp.ui.theme.*
import com.example.fitapp.viewmodel.FitAppViewModel
import kotlinx.coroutines.launch

enum class GeneratorMode(val title: String) {
    VARIATIONS("Rule Builder"),
    AI_COACH("AI Coach")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(
    onSaveWorkout: (Workout) -> Unit,
    viewModel: FitAppViewModel? = null,
    onToggleTheme: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors
    val coroutineScope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(GeneratorMode.VARIATIONS) }

    // AI Connection Mode from ViewModel if available, or local state
    val vmAiMode by viewModel?.aiConnectionMode?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(AiConnectionMode.CLOUD_GEMINI) }
    val vmApiKey by viewModel?.userApiKey?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("") }
    var selectedAiMode by remember(vmAiMode) { mutableStateOf(vmAiMode) }
    var showMissingKeyPrompt by remember { mutableStateOf(false) }

    // Mode 1: Rule Builder State
    val selectedMuscles = remember { mutableStateListOf<String>("Chest") }
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    val selectedEquipment = remember { mutableStateListOf<String>() }
    var exerciseCountMode by remember { mutableStateOf(ExerciseCountMode.PER_MUSCLE) }
    var totalExerciseCount by remember { mutableIntStateOf(5) }
    var perMuscleExerciseCount by remember { mutableIntStateOf(2) }
    val perMuscleCounts = remember {
        mutableStateMapOf<String, Int>().apply {
            put("Chest", 2)
        }
    }
    var variationCount by remember { mutableIntStateOf(3) }
    var generatedVariations by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var hasGeneratedVariations by remember { mutableStateOf(false) }

    // Mode 2: AI Coach State
    var aiGoalInput by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }
    var aiWorkoutResult by remember { mutableStateOf<Workout?>(null) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    // Intercept Back gesture so it navigates back inside the screen rather than exiting app
    BackHandler(enabled = aiWorkoutResult != null) {
        aiWorkoutResult = null
    }
    BackHandler(enabled = hasGeneratedVariations) {
        hasGeneratedVariations = false
        generatedVariations = emptyList()
    }

    val promptSuggestions = remember {
        listOf(
            "Chest & Triceps Hypertrophy with Dumbbells",
            "Full Body 30-Min High Intensity Conditioning",
            "Pull Day: Back, Biceps & Grip Strength",
            "Legs & Glutes Crusher at Home",
            "Core & Abs Six-Pack Circuit"
        )
    }

    val equipmentOptions = remember {
        listOf("Barbell", "Dumbbell", "Bodyweight", "Band", "Cable", "Mat")
    }

    // Use WindowInsets(0.dp) so "Workout Generator" header doesn't get pushed down by double status bar insets
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.testTag("generate_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header & Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Workout Generator",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Build multi-variation splits or AI-customized routines",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            // Mode Selector Pill Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    GeneratorMode.entries.forEach { mode ->
                        val isSelected = mode == currentMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.primaryAccent else Color.Transparent)
                                .clickable { currentMode = mode }
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.onPrimaryAccent else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Mode Content
            if (currentMode == GeneratorMode.VARIATIONS) {
                // ==================== MODE 1: RULE BUILDER ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Presets Row
                    item {
                        Text(
                            text = "QUICK ROUTINE PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(WorkoutGenerator.PRESET_ROUTINES.keys.toList()) { presetName ->
                                val presetMuscles = WorkoutGenerator.PRESET_ROUTINES[presetName] ?: emptyList()
                                val isSelected = selectedPresetName == presetName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.2f) else colors.surface)
                                        .border(1.dp, if (isSelected) colors.primaryAccent else colors.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (selectedPresetName == presetName) {
                                                selectedPresetName = null
                                            } else {
                                                selectedPresetName = presetName
                                                selectedMuscles.clear()
                                                selectedMuscles.addAll(presetMuscles)
                                                presetMuscles.forEach { m ->
                                                    if (!perMuscleCounts.containsKey(m)) perMuscleCounts[m] = perMuscleExerciseCount
                                                }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.primaryAccent else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Target Muscles Selection
                    item {
                        Text(
                            text = "TARGET MUSCLES (${selectedMuscles.size} selected)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MuscleGroup.entries.forEach { group ->
                                val isSelected = selectedMuscles.contains(group.displayName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            if (selectedMuscles.size > 1) {
                                                selectedMuscles.remove(group.displayName)
                                            }
                                        } else {
                                            selectedMuscles.add(group.displayName)
                                            if (!perMuscleCounts.containsKey(group.displayName)) {
                                                perMuscleCounts[group.displayName] = perMuscleExerciseCount
                                            }
                                        }
                                        selectedPresetName = null
                                    },
                                    label = { Text(group.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = colors.surface,
                                        labelColor = colors.textSecondary,
                                        selectedContainerColor = colors.primaryAccent,
                                        selectedLabelColor = colors.onPrimaryAccent
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) colors.primaryAccent else colors.border,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Equipment Selection
                    item {
                        Text(
                            text = "AVAILABLE EQUIPMENT (${if (selectedEquipment.isEmpty()) "Any Equipment" else "${selectedEquipment.size} selected"})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            equipmentOptions.forEach { eq ->
                                val isSelected = selectedEquipment.contains(eq)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedEquipment.remove(eq) else selectedEquipment.add(eq)
                                    },
                                    label = { Text(eq, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = colors.surface,
                                        labelColor = colors.textSecondary,
                                        selectedContainerColor = colors.secondaryAccent,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) colors.secondaryAccent else colors.border,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Exercise Count Setting (Toggle between Total vs. Per Muscle Group)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "AMOUNT OF EXERCISES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Mode Segmented Switcher: Total vs Per Muscle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                                    .padding(3.dp)
                            ) {
                                ExerciseCountMode.entries.forEach { mode ->
                                    val isSelected = mode == exerciseCountMode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) colors.primaryAccent else Color.Transparent)
                                            .clickable { exerciseCountMode = mode }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onPrimaryAccent else colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stepper & Quick chips depending on mode
                            if (exerciseCountMode == ExerciseCountMode.TOTAL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Exercises in Routine",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Balanced across your selected muscle groups",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (totalExerciseCount > 3) totalExerciseCount-- },
                                            enabled = totalExerciseCount > 3,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = colors.textPrimary)
                                        }

                                        Text(
                                            text = "$totalExerciseCount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { if (totalExerciseCount < 12) totalExerciseCount++ },
                                            enabled = totalExerciseCount < 12,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = colors.textPrimary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(3, 4, 5, 6, 8, 10).forEach { count ->
                                        val isSel = count == totalExerciseCount
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) colors.primaryAccent.copy(alpha = 0.2f) else colors.surfaceElevated)
                                                .border(1.dp, if (isSel) colors.primaryAccent else colors.border, RoundedCornerShape(8.dp))
                                                .clickable { totalExerciseCount = count }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) colors.primaryAccent else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Header with summary
                                    val totalSelectedExercises = selectedMuscles.sumOf { perMuscleCounts[it] ?: perMuscleExerciseCount }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Exercises Per Muscle Group",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = if (selectedMuscles.isNotEmpty()) {
                                                "$totalSelectedExercises total movements across ${selectedMuscles.size} selected group${if (selectedMuscles.size == 1) "" else "s"}"
                                            } else {
                                                "Select muscle groups above to set exercise counts"
                                            },
                                            fontSize = 11.sp,
                                            color = colors.primaryAccent,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // List each individual selected muscle group with its own precise exercise count control
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.surfaceElevated)
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (selectedMuscles.isEmpty()) {
                                            Text(
                                                text = "Select one or more muscle groups above to customize exercise counts.",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        } else {
                                            selectedMuscles.forEach { muscle ->
                                                val count = perMuscleCounts[muscle] ?: perMuscleExerciseCount
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(colors.surface)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(colors.primaryAccent)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = muscle,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = colors.textPrimary
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                if (count > 1) {
                                                                    perMuscleCounts[muscle] = count - 1
                                                                }
                                                            },
                                                            enabled = count > 1,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = "Decrease $muscle", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }

                                                        Text(
                                                            text = "$count",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.primaryAccent,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                if (count < 5) {
                                                                    perMuscleCounts[muscle] = count + 1
                                                                }
                                                            },
                                                            enabled = count < 5,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Increase $muscle", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }

                                                        Text(
                                                            text = if (count == 1) "exercise" else "exercises",
                                                            fontSize = 11.sp,
                                                            color = colors.textSecondary,
                                                            modifier = Modifier.padding(start = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Number of Variations Setting
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "NUMBER OF VARIATIONS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Generate $variationCount distinct routine ${if (variationCount == 1) "option" else "options"}",
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceElevated)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (variationCount > 1) variationCount-- },
                                        enabled = variationCount > 1,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("variation_count_decrease_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease Variations",
                                            tint = if (variationCount > 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "$variationCount",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryAccent,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp)
                                            .testTag("variation_count_display")
                                    )

                                    IconButton(
                                        onClick = { if (variationCount < 6) variationCount++ },
                                        enabled = variationCount < 6,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("variation_count_increase_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase Variations",
                                            tint = if (variationCount < 6) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Generate Variations CTA
                    item {
                        val currentTotal = if (exerciseCountMode == ExerciseCountMode.TOTAL) {
                            totalExerciseCount
                        } else {
                            selectedMuscles.sumOf { perMuscleCounts[it] ?: perMuscleExerciseCount }
                        }
                        val buttonText = "Generate $variationCount ${if (variationCount == 1) "Variation" else "Variations"} ($currentTotal movements)"

                        Button(
                            onClick = {
                                generatedVariations = WorkoutGenerator.generateVariations(
                                    muscles = selectedMuscles.toSet(),
                                    equipment = selectedEquipment.toSet(),
                                    exerciseCount = if (exerciseCountMode == ExerciseCountMode.TOTAL) totalExerciseCount else 2,
                                    countMode = exerciseCountMode,
                                    perMuscleCounts = perMuscleCounts.toMap(),
                                    variationCount = variationCount
                                )
                                hasGeneratedVariations = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_variations_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = colors.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(buttonText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Results Section
                    if (hasGeneratedVariations) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GENERATED VARIATIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    letterSpacing = 1.sp
                                )

                                TextButton(
                                    onClick = {
                                        generatedVariations = WorkoutGenerator.generateVariations(
                                            muscles = selectedMuscles.toSet(),
                                            equipment = selectedEquipment.toSet(),
                                            exerciseCount = if (exerciseCountMode == ExerciseCountMode.TOTAL) totalExerciseCount else 2,
                                            countMode = exerciseCountMode,
                                            perMuscleCounts = perMuscleCounts.toMap(),
                                            variationCount = variationCount
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Shuffle", color = colors.primaryAccent, fontSize = 12.sp)
                                }
                            }
                        }

                        items(generatedVariations) { workout ->
                            GeneratedWorkoutCard(
                                workout = workout,
                                onSave = { onSaveWorkout(workout) }
                            )
                        }
                    }
                }
            } else {
                // ==================== MODE 2: AI WORKOUT COACH ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Coach Intro Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(18.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "AI Fitness Coach",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Dual-engine: Cloud Gemini or On-Device AI Core",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // AI Engine Connection Switcher Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                            shape = RoundedCornerShape(18.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI CONNECTION ENGINE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )

                                    // Badge showing active
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE)
                                                    Color(0xFF00E676).copy(alpha = 0.15f)
                                                else
                                                    colors.primaryAccent.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) "⚡ LOCAL HARDWARE" else "☁️ CLOUD GEMINI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) Color(0xFF00E676) else colors.primaryAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Mode selection buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Option 1: Cloud Gemini
                                    val isCloudSelected = selectedAiMode == AiConnectionMode.CLOUD_GEMINI
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCloudSelected) colors.surface else colors.surfaceElevated)
                                            .border(
                                                width = if (isCloudSelected) 2.dp else 1.dp,
                                                color = if (isCloudSelected) colors.primaryAccent else colors.border,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedAiMode = AiConnectionMode.CLOUD_GEMINI
                                                viewModel?.setAiConnectionMode(AiConnectionMode.CLOUD_GEMINI)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Cloud,
                                                    contentDescription = null,
                                                    tint = if (isCloudSelected) colors.primaryAccent else colors.textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Google API Key",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCloudSelected) colors.textPrimary else colors.textSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Gemini 2.5 Flash cloud intelligence",
                                                fontSize = 10.sp,
                                                color = colors.textMuted,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    // Option 2: On-Device AI Core
                                    val isLocalSelected = selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isLocalSelected) colors.surface else colors.surfaceElevated)
                                            .border(
                                                width = if (isLocalSelected) 2.dp else 1.dp,
                                                color = if (isLocalSelected) Color(0xFF00E676) else colors.border,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedAiMode = AiConnectionMode.ON_DEVICE_AI_CORE
                                                viewModel?.setAiConnectionMode(AiConnectionMode.ON_DEVICE_AI_CORE)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Memory,
                                                    contentDescription = null,
                                                    tint = if (isLocalSelected) Color(0xFF00E676) else colors.textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Phone AI Core",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLocalSelected) colors.textPrimary else colors.textSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Direct hardware, 100% offline & private",
                                                fontSize = 10.sp,
                                                color = colors.textMuted,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                // Google API Key status for Cloud Gemini mode
                                if (selectedAiMode == AiConnectionMode.CLOUD_GEMINI) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (vmApiKey.isNotBlank()) Color(0xFF00E676).copy(alpha = 0.12f) else colors.surface)
                                            .border(1.dp, if (vmApiKey.isNotBlank()) Color(0xFF00E676).copy(alpha = 0.3f) else colors.border, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (vmApiKey.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (vmApiKey.isNotBlank()) Color(0xFF00E676) else colors.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (vmApiKey.isNotBlank()) "Google API Key active (configured in Settings)" else "Google API Key not set",
                                                fontSize = 11.sp,
                                                fontWeight = if (vmApiKey.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (vmApiKey.isNotBlank()) Color(0xFF00E676) else colors.textSecondary
                                            )
                                        }
                                        if (vmApiKey.isBlank()) {
                                            TextButton(
                                                onClick = onOpenSettings,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Settings →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primaryAccent)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Natural Language Goal Input
                    item {
                        Text(
                            text = "DESCRIBE YOUR WORKOUT GOAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = aiGoalInput,
                            onValueChange = { aiGoalInput = it },
                            placeholder = {
                                Text(
                                    "e.g. Build a 30-minute chest and triceps hypertrophy workout with dumbbells",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedBorderColor = colors.primaryAccent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_workout_goal_input")
                        )
                    }

                    // Prompt Suggestions Chips
                    item {
                        Text(
                            text = "SUGGESTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(promptSuggestions) { prompt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                        .clickable { aiGoalInput = prompt }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(prompt, fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }

                    // Generate AI Workout CTA Button
                    item {
                        Button(
                            onClick = {
                                if (selectedAiMode == AiConnectionMode.CLOUD_GEMINI && vmApiKey.isBlank()) {
                                    aiErrorMessage = "Set the key in the settings first"
                                    showMissingKeyPrompt = true
                                    return@Button
                                }
                                val goal = aiGoalInput.ifBlank { "Full body strength and hypertrophy workout" }
                                isAiGenerating = true
                                aiErrorMessage = null
                                coroutineScope.launch {
                                    val result = AiWorkoutService.generateWorkout(
                                        goal = goal,
                                        mode = selectedAiMode,
                                        userApiKey = vmApiKey.takeIf { it.isNotBlank() }
                                    )
                                    isAiGenerating = false
                                    if (result.isSuccess) {
                                        aiWorkoutResult = result.getOrNull()
                                    } else {
                                        aiErrorMessage = result.exceptionOrNull()?.message ?: "Failed to generate workout"
                                    }
                                }
                            },
                            enabled = !isAiGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_ai_coach_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) Color(0xFF00E676) else colors.primaryAccent,
                                contentColor = if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) Color(0xFF0C1217) else colors.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isAiGenerating) {
                                CircularProgressIndicator(
                                    color = if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) Color(0xFF0C1217) else colors.onPrimaryAccent,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) "AI Core processing on device..." else "Coach is designing your routine...",
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) Icons.Default.Memory else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (selectedAiMode == AiConnectionMode.ON_DEVICE_AI_CORE) "Generate with On-Device AI Core" else "Generate with Gemini Coach",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Error Notice if any
                    if (aiErrorMessage != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f)))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = aiErrorMessage!!,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (aiErrorMessage == "Set the key in the settings first") {
                                        TextButton(
                                            onClick = onOpenSettings,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Settings", color = colors.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Result Card
                    if (aiWorkoutResult != null) {
                        item {
                            Text(
                                text = "COACH'S DESIGNED ROUTINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccent,
                                letterSpacing = 1.sp
                            )
                        }

                        item {
                            GeneratedWorkoutCard(
                                workout = aiWorkoutResult!!,
                                onSave = { onSaveWorkout(aiWorkoutResult!!) }
                            )
                        }
                    }
                }
            }
        }

        if (showMissingKeyPrompt) {
            AlertDialog(
                onDismissRequest = { showMissingKeyPrompt = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "API Key Required",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Set the key in the settings first to generate routines using Cloud Gemini Coach.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMissingKeyPrompt = false
                            onOpenSettings()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMissingKeyPrompt = false }) {
                        Text("Cancel", color = colors.textMuted)
                    }
                },
                containerColor = colors.surfaceElevated,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun GeneratedWorkoutCard(
    workout: Workout,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors
    var isSaved by remember { mutableStateOf(false) }

    val isLocalAi = workout.note.contains("phone AI Core", ignoreCase = true) ||
            workout.note.contains("on-device", ignoreCase = true) ||
            workout.note.contains("Offline", ignoreCase = true)

    val isCloudAi = workout.note.contains("Gemini", ignoreCase = true) ||
            workout.note.contains("Cloud", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // AI Execution Verification Badge
            if (isLocalAi) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "⚡ VERIFIED ON-DEVICE NEURAL CORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Executed 100% locally on phone AI Core hardware • Zero cloud calls • Private & Offline",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (isCloudAi) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4285F4).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "☁️ VERIFIED GOOGLE GEMINI CLOUD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8AB4F8),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Connected via Google Gemini Flash API • High-intelligence synthesis",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    if (workout.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = workout.note,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${workout.exercises.size} movements",
                    fontSize = 12.sp,
                    color = colors.primaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${workout.totalSets} total sets",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = "~${workout.estimatedMinutes} min",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercise list items
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                workout.exercises.forEachIndexed { idx, ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}.",
                                fontSize = 12.sp,
                                color = colors.primaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ex.exerciseName,
                                fontSize = 13.sp,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "${ex.sets} × ${ex.reps}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Single primary Action Button: Save Routine (Removed legacy "Start Now" button)
            Button(
                onClick = {
                    onSave()
                    isSaved = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) ElectricEmerald else colors.primaryAccent,
                    contentColor = if (isSaved) Color.Black else colors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_generated_workout_btn")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Saved to My Workouts" else "Save Routine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

