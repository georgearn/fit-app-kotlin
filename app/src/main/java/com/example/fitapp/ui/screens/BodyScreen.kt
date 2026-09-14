package com.example.fitapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.model.Exercise
import com.example.fitapp.ui.components.AnatomicalBodyMap
import com.example.fitapp.ui.components.BodyOrientation
import com.example.fitapp.ui.components.ExerciseCard
import com.example.fitapp.ui.components.ExerciseDetailSheet
import com.example.fitapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    onAddToWorkout: ((Exercise) -> Unit)? = null,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var isViewingExerciseList by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf(BodyOrientation.FRONT) }
    val isFront = orientation == BodyOrientation.FRONT

    var selectedEquipmentFilter by remember { mutableStateOf("All") }
    var detailedExercise by remember { mutableStateOf<Exercise?>(null) }

    // Intercept Back gesture so back navigates logically without exiting the app
    BackHandler(enabled = detailedExercise != null) {
        detailedExercise = null
    }
    BackHandler(enabled = isViewingExerciseList) {
        isViewingExerciseList = false
    }
    BackHandler(enabled = !isViewingExerciseList && selectedMuscle != null) {
        selectedMuscle = null
    }

    val equipmentOptions = remember {
        listOf("All", "Barbell", "Dumbbell", "Bodyweight", "Cable", "Machine", "Band", "Kettlebell")
    }

    val exercises = remember(searchQuery, selectedMuscle, selectedEquipmentFilter) {
        var list = if (searchQuery.isNotBlank()) {
            ExerciseCatalog.search(searchQuery)
        } else if (!selectedMuscle.isNullOrBlank()) {
            ExerciseCatalog.getByMuscle(selectedMuscle!!)
        } else {
            ExerciseCatalog.allExercises
        }

        if (selectedEquipmentFilter != "All") {
            list = list.filter { ex ->
                ex.equipment.contains(selectedEquipmentFilter, ignoreCase = true) ||
                ex.equipmentList.any { it.contains(selectedEquipmentFilter, ignoreCase = true) }
            }
        }

        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("body_screen")
    ) {
        if (!isViewingExerciseList) {
            // ==========================================
            // MAIN BODY MAP PAGE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotBlank()) {
                            isViewingExerciseList = true
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search exercises...",
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
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
                        .height(52.dp)
                        .testTag("exercise_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Pill Toggle: [✓ Front | Back]
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Front Button
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isFront) colors.primaryAccent.copy(alpha = if (colors.isDark) 0.28f else 0.15f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { orientation = BodyOrientation.FRONT }
                            .padding(horizontal = 22.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isFront) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = "Front",
                            fontSize = 13.sp,
                            fontWeight = if (isFront) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isFront) colors.primaryAccent else colors.textSecondary
                        )
                    }

                    // Back Button
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isFront) colors.primaryAccent.copy(alpha = if (colors.isDark) 0.28f else 0.15f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { orientation = BodyOrientation.BACK }
                            .padding(horizontal = 22.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!isFront) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = "Back",
                            fontSize = 13.sp,
                            fontWeight = if (!isFront) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!isFront) colors.primaryAccent else colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Body Map that fits the available screen height
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnatomicalBodyMap(
                        isFront = isFront,
                        selectedMuscle = selectedMuscle,
                        onMuscleTapped = { muscle ->
                            if (selectedMuscle == muscle) {
                                isViewingExerciseList = true
                            } else {
                                selectedMuscle = muscle
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Interactive Selected Muscle card or helper hint
                if (selectedMuscle != null) {
                    val muscleExercises = remember(selectedMuscle) {
                        selectedMuscle?.let { ExerciseCatalog.getByMuscle(it) } ?: emptyList()
                    }
                    Surface(
                        color = colors.surfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedMuscle ?: "",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${muscleExercises.size} exercises found",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedMuscle = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear selection",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { isViewingExerciseList = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryAccent,
                                        contentColor = colors.onPrimaryAccent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("view_muscle_exercises_btn")
                                ) {
                                    Text("Exercises", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Bottom Hint Text right above floating tab bar
                    Text(
                        text = "Tap a muscle to highlight and view exercises",
                        fontSize = 13.sp,
                        color = colors.textMuted,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        } else {
            // ==========================================
            // EXERCISE LIST VIEW (When a muscle is tapped or search active)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
            ) {
                // Top Navigation & Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isViewingExerciseList = false
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape)
                            .testTag("back_to_body_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Body Map",
                            tint = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Search: \"$searchQuery\"" else "${selectedMuscle ?: "All"} Exercises",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${exercises.size} movements available",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                // Search input in list mode
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search exercises...",
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
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
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(50.dp)
                )

                // Equipment Filter Pills
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(equipmentOptions) { eq ->
                        val isSelected = selectedEquipmentFilter == eq
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedEquipmentFilter = eq },
                            label = { Text(eq, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textSecondary,
                                selectedContainerColor = colors.primaryAccent.copy(alpha = if (colors.isDark) 0.25f else 0.15f),
                                selectedLabelColor = colors.primaryAccent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) colors.primaryAccent.copy(alpha = 0.5f) else colors.border,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = CircleShape
                        )
                    }
                }

                // Exercises List
                if (exercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No exercises found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your filter or search query",
                                fontSize = 13.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(exercises, key = { it.id }) { exercise ->
                            ExerciseCard(
                                exercise = exercise,
                                onClick = { detailedExercise = exercise },
                                onAddToWorkout = null
                            )
                        }
                    }
                }
            }
        }

        // Exercise Detail Bottom Sheet
        if (detailedExercise != null) {
            ExerciseDetailSheet(
                exercise = detailedExercise!!,
                onDismiss = { detailedExercise = null },
                onAddToWorkout = null
            )
        }
    }
}
