package com.example.fitapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    workouts: List<Workout>,
    onDeleteWorkout: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateWorkout: (Workout) -> Unit,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors
    var showManualBuilder by remember { mutableStateOf(false) }
    var editingWorkout by remember { mutableStateOf<Workout?>(null) }
    var viewingWorkout by remember { mutableStateOf<Workout?>(null) }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }
    var optionsWorkout by remember { mutableStateOf<Workout?>(null) }
    var selectedExerciseDetail by remember { mutableStateOf<Exercise?>(null) }

    // Intercept Back gesture so the app doesn't exit when viewing detail views
    BackHandler(enabled = selectedExerciseDetail != null) {
        selectedExerciseDetail = null
    }
    BackHandler(enabled = workoutToDelete != null) {
        workoutToDelete = null
    }
    BackHandler(enabled = optionsWorkout != null) {
        optionsWorkout = null
    }
    BackHandler(enabled = viewingWorkout != null) {
        viewingWorkout = null
    }
    BackHandler(enabled = showManualBuilder || editingWorkout != null) {
        showManualBuilder = false
        editingWorkout = null
    }

    // If viewing the full-page Manual Builder (Create or Edit)
    if (showManualBuilder || editingWorkout != null) {
        ManualBuilderScreen(
            initialWorkout = editingWorkout,
            onDismiss = {
                showManualBuilder = false
                editingWorkout = null
            },
            onSaveWorkout = { savedWorkout ->
                onCreateWorkout(savedWorkout)
                showManualBuilder = false
                editingWorkout = null
                if (viewingWorkout?.id == savedWorkout.id) {
                    viewingWorkout = savedWorkout
                }
            }
        )
        return
    }

    // If viewing the Workout Compilation (exercises with descriptions)
    if (viewingWorkout != null) {
        WorkoutCompilationView(
            workout = viewingWorkout!!,
            onBack = { viewingWorkout = null },
            onEdit = {
                editingWorkout = viewingWorkout
            },
            onDelete = {
                workoutToDelete = viewingWorkout
                viewingWorkout = null
            },
            onToggleFavorite = {
                onToggleFavorite(viewingWorkout!!.id)
                // update local state
                viewingWorkout = viewingWorkout!!.copy(isFavorite = !viewingWorkout!!.isFavorite)
            },
            onExerciseClick = { exercise ->
                selectedExerciseDetail = exercise
            }
        )

        if (selectedExerciseDetail != null) {
            ExerciseDetailSheet(
                exercise = selectedExerciseDetail!!,
                onDismiss = { selectedExerciseDetail = null },
                onAddToWorkout = {},
                onSelectModification = { replacement ->
                    val currentW = viewingWorkout
                    if (currentW != null) {
                        val updatedExercises = currentW.exercises.map { we ->
                            if (we.exerciseId == selectedExerciseDetail!!.id) {
                                we.copy(
                                    exerciseId = replacement.id,
                                    exerciseName = replacement.name,
                                    muscle = replacement.muscle,
                                    equipment = replacement.equipment,
                                    imageAsset = replacement.imageAsset
                                )
                            } else we
                        }
                        val updatedWorkout = currentW.copy(exercises = updatedExercises)
                        viewingWorkout = updatedWorkout
                        onCreateWorkout(updatedWorkout)
                    }
                }
            )
        }
        return
    }

    // Default Workouts List - set contentWindowInsets to 0 so header isn't pushed down by double status bar insets
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.testTag("workouts_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header (Theme toggle centralized in navigation bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Workouts",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${workouts.size} exercise compilations",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Button(
                    onClick = { showManualBuilder = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = colors.onPrimaryAccent
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("create_custom_workout_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Routine", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Workouts List
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No routines yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'New Routine' to build your exercise compilation!",
                            fontSize = 13.sp,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(workouts, key = { it.id }) { workout ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart && !workout.isPreset) {
                                    workoutToDelete = workout
                                    false
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = !workout.isPreset,
                            backgroundContent = {
                                val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSwiping) Color(0xFFD32F2F) else Color(0xFFC62828).copy(alpha = 0.85f))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Delete",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete routine",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            WorkoutCompilationCard(
                                workout = workout,
                                onClick = { viewingWorkout = workout },
                                onLongClick = { optionsWorkout = workout },
                                onEdit = { editingWorkout = workout },
                                onToggleFavorite = { onToggleFavorite(workout.id) },
                                onDelete = { workoutToDelete = workout }
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-tap Options Bottom Sheet
    if (optionsWorkout != null) {
        val target = optionsWorkout!!
        ModalBottomSheet(
            onDismissRequest = { optionsWorkout = null },
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${target.exercises.size} movements · ${target.targetMuscles.joinToString()}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    if (target.isPreset) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primaryAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("PRESET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primaryAccent)
                        }
                    }
                }

                HorizontalDivider(color = colors.border)

                // Option 1: Edit Routine
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val toEdit = target
                            optionsWorkout = null
                            editingWorkout = toEdit
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Edit Routine", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Add, remove, or swap exercises", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }

                // Option 2: Duplicate Routine
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val clone = target.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                name = "${target.name} (Copy)",
                                isPreset = false
                            )
                            onCreateWorkout(clone)
                            optionsWorkout = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Duplicate Routine", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Create a customizable clone", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }

                // Option: Delete Routine (if not preset)
                if (!target.isPreset) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val toDelete = target
                                optionsWorkout = null
                                workoutToDelete = toDelete
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Delete Routine", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF5252))
                            Text("Permanently remove this saved routine", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (workoutToDelete != null) {
        val toDelete = workoutToDelete!!
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Delete Routine?",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${toDelete.name}'? This workout compilation will be permanently removed.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = toDelete.id
                        workoutToDelete = null
                        onDeleteWorkout(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

/**
 * Clean card showing a compilation of exercises per workout
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutCompilationCard(
    workout: Workout,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("workout_card_${workout.id}"),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Title, Edit, Fav & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workout.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (workout.isPreset) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.primaryAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PRESET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.primaryAccent)
                            }
                        }
                    }

                    if (workout.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = workout.note,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit routine",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!workout.isPreset) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete routine",
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges: Count & Muscles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${workout.exercises.size} exercises",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryAccent
                    )
                }

                workout.targetMuscles.take(3).forEach { muscle ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(muscle, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercise Preview Thumbnails
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(workout.exercises.take(6)) { ex ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ex.displayAssetPath.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(ex.displayAssetPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = ex.exerciseName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gestures hint & "View Exercises" Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to open · Hold or swipe to edit/delete",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                Text(
                    text = "View details →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryAccent
                )
            }
        }
    }
}

/**
 * Full page Workout Compilation View:
 * "The point is to simply have a compilation of exercises per workout,
 * like what I should do with descriptions, nothing more"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCompilationView(
    workout: Workout,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Workouts",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workout.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit routine",
                            tint = colors.primaryAccent
                        )
                    }

                    if (!workout.isPreset) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = colors.textMuted
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header summary info
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (workout.note.isNotBlank()) {
                        Text(
                            text = workout.note,
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Target muscles pill row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workout.targetMuscles.forEach { muscle ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.primaryAccent.copy(alpha = 0.15f))
                                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(muscle, fontSize = 11.sp, color = colors.primaryAccent)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EXERCISES (${workout.exercises.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Exercise Items with Descriptions
            items(workout.exercises, key = { it.exerciseId }) { workoutEx ->
                val exerciseData = remember(workoutEx.exerciseId) {
                    ExerciseCatalog.getById(workoutEx.exerciseId)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            exerciseData?.let { onExerciseClick(it) }
                        },
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (workoutEx.displayAssetPath.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(workoutEx.displayAssetPath)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = workoutEx.exerciseName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workoutEx.exerciseName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${workoutEx.muscle} · ${workoutEx.equipment}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${workoutEx.sets} sets × ${workoutEx.reps} reps",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryAccent
                                )

                                if (exerciseData != null && exerciseData.variantIds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.primaryAccent.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable { onExerciseClick(exerciseData) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                tint = colors.primaryAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${exerciseData.variantIds.size} modifications available",
                                                fontSize = 10.sp,
                                                color = colors.primaryAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Exercise Instructions ("what I should do with descriptions")
                        if (exerciseData != null && exerciseData.instructions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = colors.border, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "What to do:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            exerciseData.instructions.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${index + 1}. ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryAccent
                                    )
                                    Text(
                                        text = step,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            if (exerciseData.cues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exerciseData.cues.first(),
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
