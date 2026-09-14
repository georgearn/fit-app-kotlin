package com.example.fitapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitapp.model.Exercise
import com.example.fitapp.model.Workout
import com.example.fitapp.ui.components.AppTab
import com.example.fitapp.ui.components.FloatingPillTabBar
import com.example.fitapp.ui.components.SettingsSheet
import com.example.fitapp.ui.screens.ActiveWorkoutPlayer
import com.example.fitapp.ui.screens.BodyScreen
import com.example.fitapp.ui.screens.GenerateScreen
import com.example.fitapp.ui.screens.WorkoutsScreen
import com.example.fitapp.ui.theme.*
import com.example.fitapp.viewmodel.FitAppViewModel

@Composable
fun FitAppMain(
    viewModel: FitAppViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val exerciseToAdd by viewModel.exerciseToAddToWorkout.collectAsStateWithLifecycle()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Intercept Back gesture to prevent app from closing unexpectedly
    BackHandler(enabled = activeWorkout != null) {
        viewModel.finishActiveWorkout()
    }
    BackHandler(enabled = showSettingsSheet) {
        showSettingsSheet = false
    }
    BackHandler(enabled = exerciseToAdd != null) {
        viewModel.setExerciseToAddToWorkout(null)
    }
    BackHandler(enabled = selectedTab != AppTab.BODY && activeWorkout == null && !showSettingsSheet && exerciseToAdd == null) {
        viewModel.selectTab(AppTab.BODY)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val colors = FitTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (activeWorkout != null) {
            // Fullscreen Live Workout Session
            ActiveWorkoutPlayer(
                workout = activeWorkout!!,
                onFinish = {
                    viewModel.finishActiveWorkout()
                    snackbarMessage = "Workout completed! Great effort."
                },
                onClose = {
                    viewModel.finishActiveWorkout()
                }
            )
        } else {
            // Main Tab View with signature floating pill tab bar
            Scaffold(
                containerColor = colors.background,
                contentWindowInsets = WindowInsets.systemBars,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    FloatingPillTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onOpenSettings = { showSettingsSheet = true }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            AppTab.BODY -> {
                                BodyScreen(
                                    onToggleTheme = { viewModel.toggleTheme() }
                                )
                            }
                            AppTab.WORKOUTS -> {
                                WorkoutsScreen(
                                    workouts = workouts,
                                    onDeleteWorkout = { workoutId ->
                                        viewModel.deleteWorkout(workoutId)
                                        snackbarMessage = "Routine removed"
                                    },
                                    onToggleFavorite = { workoutId ->
                                        viewModel.toggleFavorite(workoutId)
                                    },
                                    onCreateWorkout = { newWorkout ->
                                        viewModel.saveWorkout(newWorkout)
                                        snackbarMessage = "New routine saved!"
                                    },
                                    onToggleTheme = { viewModel.toggleTheme() }
                                )
                            }
                            AppTab.GENERATE -> {
                                GenerateScreen(
                                    viewModel = viewModel,
                                    onSaveWorkout = { workout ->
                                        viewModel.saveWorkout(workout)
                                        snackbarMessage = "Saved to My Workouts!"
                                    },
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onOpenSettings = { showSettingsSheet = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Exercise to Workout Dialog
        if (exerciseToAdd != null) {
            AddExerciseToWorkoutDialog(
                exercise = exerciseToAdd!!,
                workouts = workouts,
                onDismiss = { viewModel.setExerciseToAddToWorkout(null) },
                onSelectWorkout = { workoutId ->
                    viewModel.addExerciseToWorkout(workoutId, exerciseToAdd!!)
                    snackbarMessage = "Added ${exerciseToAdd!!.name} to workout!"
                },
                onCreateWithExercise = { workoutName ->
                    val workout = Workout(
                        name = workoutName,
                        note = "Custom routine containing ${exerciseToAdd!!.name}",
                        exercises = listOf(
                            com.example.fitapp.model.WorkoutExercise(
                                exerciseId = exerciseToAdd!!.id,
                                exerciseName = exerciseToAdd!!.name,
                                muscle = exerciseToAdd!!.muscle,
                                equipment = exerciseToAdd!!.equipment,
                                sets = 3,
                                reps = "10-12",
                                restSec = 60,
                                imageAsset = exerciseToAdd!!.imageAsset
                            )
                        ),
                        targetMuscles = listOf(exerciseToAdd!!.muscle)
                    )
                    viewModel.saveWorkout(workout)
                    viewModel.setExerciseToAddToWorkout(null)
                    snackbarMessage = "Created $workoutName with ${exerciseToAdd!!.name}!"
                }
            )
        }

        if (showSettingsSheet) {
            SettingsSheet(
                viewModel = viewModel,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
fun AddExerciseToWorkoutDialog(
    exercise: Exercise,
    workouts: List<Workout>,
    onDismiss: () -> Unit,
    onSelectWorkout: (String) -> Unit,
    onCreateWithExercise: (String) -> Unit
) {
    val colors = FitTheme.colors
    var isCreatingNew by remember { mutableStateOf(false) }
    var newRoutineName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Add to Workout",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add \"${exercise.name}\" to:",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isCreatingNew) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workouts) { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceElevated)
                                    .clickable { onSelectWorkout(w.id) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(w.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    Text("${w.exercises.size} movements", fontSize = 11.sp, color = colors.textSecondary)
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryAccent)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Routine")
                    }
                } else {
                    OutlinedTextField(
                        value = newRoutineName,
                        onValueChange = { newRoutineName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primaryAccent,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { isCreatingNew = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to list", color = colors.textSecondary)
                        }

                        Button(
                            onClick = {
                                if (newRoutineName.isNotBlank()) {
                                    onCreateWithExercise(newRoutineName.trim())
                                }
                            },
                            enabled = newRoutineName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent, contentColor = colors.onPrimaryAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
