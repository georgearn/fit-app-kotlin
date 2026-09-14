package com.example.fitapp.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitapp.FitAppApplication
import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.model.Workout
import com.example.fitapp.model.WorkoutExercise
import com.example.fitapp.ui.components.ExerciseDetailSheet
import com.example.fitapp.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutPlayer(
    workout: Workout,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedImageLoader = remember(context) {
        FitAppApplication.getAnimatedImageLoader(context)
    }
    val exerciseList = remember(workout) { mutableStateListOf<WorkoutExercise>().apply { addAll(workout.exercises) } }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    val currentExercise = exerciseList.getOrNull(currentExerciseIndex)
    var showDetailOrModifications by remember { mutableStateOf(false) }
    val fullExerciseData = remember(currentExercise) { currentExercise?.let { ExerciseCatalog.getById(it.exerciseId) } }

    // Map of exerciseId to list of completed set indices
    val completedSets = remember { mutableStateMapOf<String, MutableSet<Int>>() }

    // Rest timer state
    var restSecondsLeft by remember { mutableIntStateOf(0) }
    var restTotalSeconds by remember { mutableIntStateOf(60) }
    var isResting by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Countdown effect
    LaunchedEffect(isResting, restSecondsLeft) {
        if (isResting && restSecondsLeft > 0) {
            delay(1000L)
            restSecondsLeft--
            if (restSecondsLeft == 0) {
                isResting = false
                vibratePhone(context)
            }
        }
    }

    if (currentExercise == null) {
        onClose()
        return
    }

    val currentCompleted = completedSets.getOrPut(currentExercise.exerciseId) { mutableSetOf() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("active_workout_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Exit workout",
                        tint = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = workout.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Exercise ${currentExerciseIndex + 1} of ${workout.exercises.size}",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }

                Button(
                    onClick = { showFinishDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.2f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Bar
            val progress = (currentExerciseIndex + 1).toFloat() / workout.exercises.size.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = NeonCyan,
                trackColor = DarkSurfaceElevated
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise Demo Card (White Window)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showDetailOrModifications = true },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentExercise.displayAssetPath.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentExercise.displayAssetPath)
                                .crossfade(true)
                                .build(),
                            imageLoader = animatedImageLoader,
                            contentDescription = currentExercise.exerciseName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exercise Title and Tags
            Text(
                text = currentExercise.exerciseName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = currentExercise.muscle,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated
                ) {
                    Text(
                        text = currentExercise.equipment,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Modifications Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { showDetailOrModifications = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Modifications",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (fullExerciseData?.variantIds?.isNotEmpty() == true) {
                                "${fullExerciseData.variantIds.size} Modifications"
                            } else {
                                "Form Guide"
                            },
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sets Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SETS & REPS (${currentCompleted.size}/${currentExercise.sets} completed)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                for (setIdx in 1..currentExercise.sets) {
                    val isDone = setIdx in currentCompleted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDone) NeonCyan.copy(alpha = 0.1f) else DarkSurface)
                            .border(1.dp, if (isDone) NeonCyan.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                if (isDone) {
                                    currentCompleted.remove(setIdx)
                                } else {
                                    currentCompleted.add(setIdx)
                                    // Trigger Rest Timer
                                    restTotalSeconds = currentExercise.restSec
                                    restSecondsLeft = currentExercise.restSec
                                    isResting = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Set $setIdx",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) NeonCyan else TextPrimary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${currentExercise.reps} reps",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isDone) "Completed" else "Incomplete",
                            tint = if (isDone) NeonCyan else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentExerciseIndex > 0) {
                            currentExerciseIndex--
                            isResting = false
                        }
                    },
                    enabled = currentExerciseIndex > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (currentExerciseIndex < workout.exercises.size - 1) {
                            currentExerciseIndex++
                            isResting = false
                        } else {
                            showFinishDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (currentExerciseIndex < workout.exercises.size - 1) "Next" else "Finish",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Rest Timer Floating Overlay
        AnimatedVisibility(
            visible = isResting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REST INTERVAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }

                        TextButton(onClick = { isResting = false }) {
                            Text("Skip Rest", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${restSecondsLeft}s",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { restSecondsLeft = (restSecondsLeft - 15).coerceAtLeast(0) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("-15s", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { restSecondsLeft += 15 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+15s", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Finish Celebration Dialog
        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberFire)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Workout Complete!", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    val totalSets = workout.exercises.sumOf { completedSets[it.exerciseId]?.size ?: 0 }
                    Column {
                        Text(
                            text = "Awesome work! You completed $totalSets sets across ${workout.exercises.size} movements.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Target muscles worked: ${workout.targetMuscles.joinToString(", ")}",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFinishDialog = false
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text("Keep Training", color = TextSecondary)
                    }
                }
            )
        }

        if (showDetailOrModifications && fullExerciseData != null) {
            ExerciseDetailSheet(
                exercise = fullExerciseData,
                onDismiss = { showDetailOrModifications = false },
                onAddToWorkout = {},
                onSelectModification = { mod ->
                    exerciseList[currentExerciseIndex] = currentExercise.copy(
                        exerciseId = mod.id,
                        exerciseName = mod.name,
                        muscle = mod.muscle,
                        equipment = mod.equipment,
                        imageAsset = mod.imageAsset
                    )
                    showDetailOrModifications = false
                }
            )
        }
    }
}

private fun vibratePhone(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
