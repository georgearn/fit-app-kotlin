package com.example.fitapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.model.Exercise
import com.example.fitapp.model.Workout
import com.example.fitapp.model.WorkoutExercise
import com.example.fitapp.ui.components.ExerciseDetailSheet
import com.example.fitapp.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualBuilderScreen(
    initialWorkout: Workout? = null,
    onDismiss: () -> Unit,
    onSaveWorkout: (Workout) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = initialWorkout != null
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedEquipment by remember { mutableStateOf("any") }
    var showEquipmentDropdown by remember { mutableStateOf(false) }

    var routineName by remember { mutableStateOf(initialWorkout?.name ?: "") }
    var showNameDialog by remember { mutableStateOf(false) }
    var previewExercise by remember { mutableStateOf<Exercise?>(null) }

    val addedExercises = remember {
        mutableStateListOf<Exercise>().apply {
            if (initialWorkout != null) {
                val loaded = initialWorkout.exercises.map { we ->
                    ExerciseCatalog.getById(we.exerciseId) ?: Exercise(
                        id = we.exerciseId,
                        name = we.exerciseName,
                        muscle = we.muscle,
                        equipment = we.equipment,
                        imageAsset = we.imageAsset
                    )
                }
                addAll(loaded)
            }
        }
    }

    val muscleRow1 = remember { listOf("Chest", "Upper Back", "Traps", "Lower Back", "Shoulders") }
    val muscleRow2 = remember { listOf("Neck", "Biceps", "Triceps", "Forearms", "Legs") }
    val muscleRow3 = remember { listOf("Adductors", "Calves", "Glutes", "Core", "Obliques") }

    val equipmentOptions = remember {
        listOf("any", "Barbell", "Dumbbell", "Bodyweight", "Cable", "Machine", "Band", "Kettlebell")
    }

    // Filter exercises based on search, selected muscle and equipment
    val filteredExercises = remember(searchQuery, selectedMuscle, selectedEquipment) {
        var list = if (searchQuery.isNotBlank()) {
            ExerciseCatalog.search(searchQuery)
        } else if (!selectedMuscle.isNullOrBlank()) {
            ExerciseCatalog.getByMuscle(selectedMuscle!!)
        } else {
            ExerciseCatalog.allExercises
        }

        if (selectedEquipment != "any") {
            list = list.filter { ex ->
                ex.equipment.contains(selectedEquipment, ignoreCase = true) ||
                ex.equipmentList.any { it.contains(selectedEquipment, ignoreCase = true) }
            }
        }
        list
    }

    Scaffold(
        containerColor = Color(0xFF0C1217),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("manual_builder_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEditing) "Edit Routine" else "Manual Builder",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Button (↺)
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            selectedMuscle = null
                            selectedEquipment = "any"
                        },
                        modifier = Modifier.testTag("manual_builder_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset filters",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (addedExercises.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C1217))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { showNameDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF0C1217)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_workout_final_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Workout (${addedExercises.size} exercises)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.testTag("manual_builder_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search exercises...",
                            color = Color(0xFF677785),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8899A6), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131A21),
                        unfocusedContainerColor = Color(0xFF131A21),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF222F38),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("manual_builder_search_input")
                )
            }

            // 2. Muscle Group Section (Exact 3-row layout from screenshot)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Muscle Group",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8E9FA9)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: Chest, Upper Back, Traps, Lower Back, Shoulders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        muscleRow1.forEach { muscle ->
                            MuscleChip(
                                title = muscle,
                                isSelected = selectedMuscle == muscle,
                                onToggle = {
                                    selectedMuscle = if (selectedMuscle == muscle) null else muscle
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Neck, Biceps, Triceps, Forearms, Legs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        muscleRow2.forEach { muscle ->
                            MuscleChip(
                                title = muscle,
                                isSelected = selectedMuscle == muscle,
                                onToggle = {
                                    selectedMuscle = if (selectedMuscle == muscle) null else muscle
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Adductors, Calves, Glutes, Core, Obliques
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        muscleRow3.forEach { muscle ->
                            MuscleChip(
                                title = muscle,
                                isSelected = selectedMuscle == muscle,
                                onToggle = {
                                    selectedMuscle = if (selectedMuscle == muscle) null else muscle
                                }
                            )
                        }
                    }
                }
            }

            // 3. Equipment Row (Exact layout from screenshot)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Equipment",
                        fontSize = 14.sp,
                        color = Color(0xFF8E9FA9)
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showEquipmentDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedEquipment,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select equipment",
                                tint = Color(0xFF8E9FA9),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showEquipmentDropdown,
                            onDismissRequest = { showEquipmentDropdown = false },
                            modifier = Modifier.background(Color(0xFF161F26))
                        ) {
                            equipmentOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        selectedEquipment = opt
                                        showEquipmentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(color = Color(0xFF1D2831), thickness = 1.dp)
            }

            // 4. Section: Add exercises
            item {
                Text(
                    text = "Add exercises",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Filtered exercises list with (+) button
            items(filteredExercises.take(30), key = { it.id }) { exercise ->
                val isAlreadyAdded = addedExercises.any { it.id == exercise.id }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF131A21))
                        .border(1.dp, Color(0xFF1E2A34), RoundedCornerShape(14.dp))
                        .clickable { previewExercise = exercise }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercise Image Thumbnail
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C1217)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (exercise.displayAssetPath.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(exercise.displayAssetPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Subtitle (Muscle · Equipment · Modifications)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${exercise.muscle} · ${exercise.equipment}",
                                fontSize = 12.sp,
                                color = Color(0xFF7E8F9B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (exercise.variantIds.isNotEmpty()) {
                                Text(
                                    text = "• ${exercise.variantIds.size} mods",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    // (+) Add button
                    IconButton(
                        onClick = {
                            addedExercises.add(exercise)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("add_exercise_to_builder_${exercise.id}")
                    ) {
                        Icon(
                            imageVector = if (isAlreadyAdded) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = "Add to workout",
                            tint = if (isAlreadyAdded) NeonCyan else Color(0xFF8E9FA9),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 5. Section: Your workout
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your workout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${addedExercises.size} exercises",
                        fontSize = 12.sp,
                        color = Color(0xFF7E8F9B)
                    )
                }
            }

            if (addedExercises.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10171D))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No exercises yet. Tap + on any exercise above.",
                            fontSize = 13.sp,
                            color = Color(0xFF677785)
                        )
                    }
                }
            } else {
                items(addedExercises, key = { it.id + "_" + addedExercises.indexOf(it) }) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131A21))
                            .border(1.dp, Color(0xFF1E2A34), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C1217)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (exercise.displayAssetPath.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(exercise.displayAssetPath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = exercise.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${exercise.muscle} · 3 sets",
                                fontSize = 11.sp,
                                color = Color(0xFF7E8F9B)
                            )
                        }

                        IconButton(
                            onClick = { addedExercises.remove(exercise) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove exercise",
                                tint = Color(0xFF8E9FA9),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Save Workout Name Confirmation Dialog
    if (showNameDialog) {
        val defaultName = remember(selectedMuscle) {
            if (!selectedMuscle.isNullOrBlank()) "$selectedMuscle Routine" else "Custom Workout"
        }
        var customName by remember { mutableStateOf(if (routineName.isNotBlank()) routineName else defaultName) }
        var routineNote by remember { mutableStateOf(initialWorkout?.note ?: "") }

        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF141C22),
            title = {
                Text(
                    text = if (isEditing) "Update Routine" else "Name your routine",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Compilation of ${addedExercises.size} exercises",
                        color = Color(0xFF8E9FA9),
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1217),
                            unfocusedContainerColor = Color(0xFF0C1217),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF222F38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = routineNote,
                        onValueChange = { routineNote = it },
                        label = { Text("Notes / Target focus (optional)") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1217),
                            unfocusedContainerColor = Color(0xFF0C1217),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF222F38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val workoutExercises = addedExercises.mapIndexed { idx, ex ->
                            WorkoutExercise(
                                exerciseId = ex.id,
                                exerciseName = ex.name,
                                muscle = ex.muscle,
                                equipment = ex.equipment,
                                sets = 3,
                                reps = "10-12",
                                restSec = 60,
                                imageAsset = ex.imageAsset
                            )
                        }

                        val targetMuscles = addedExercises.map { it.muscle }.distinct()
                        val newWorkout = Workout(
                            id = if (initialWorkout != null && !initialWorkout.isPreset) initialWorkout.id else UUID.randomUUID().toString(),
                            name = customName.ifBlank { if (isEditing) (initialWorkout?.name ?: "Custom Routine") else "Custom Routine" },
                            targetMuscles = targetMuscles,
                            exercises = workoutExercises,
                            isFavorite = initialWorkout?.isFavorite ?: false,
                            isPreset = false,
                            note = routineNote.ifBlank { "Compilation of ${workoutExercises.size} exercises with descriptions" }
                        )

                        onSaveWorkout(newWorkout)
                        showNameDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0C1217)
                    )
                ) {
                    Text(if (isEditing) "Save Changes" else "Save Routine", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = Color(0xFF8E9FA9))
                }
            }
        )
    }

    if (previewExercise != null) {
        ExerciseDetailSheet(
            exercise = previewExercise!!,
            onDismiss = { previewExercise = null },
            onAddToWorkout = { selected ->
                if (addedExercises.none { it.id == selected.id }) {
                    addedExercises.add(selected)
                }
                previewExercise = null
            }
        )
    }
}

@Composable
private fun MuscleChip(
    title: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF184250) else Color(0xFF172027))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF267086) else Color(0xFF23303A),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) NeonCyan else Color(0xFFB0BFC9)
        )
    }
}
