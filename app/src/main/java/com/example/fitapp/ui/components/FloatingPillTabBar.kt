package com.example.fitapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitapp.ui.theme.*

import androidx.compose.material.icons.filled.Settings

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    BODY("Body", Icons.Default.AccessibilityNew, "tab_body"),
    WORKOUTS("Workouts", Icons.Default.FitnessCenter, "tab_workouts"),
    GENERATE("Generate", Icons.Default.AutoAwesome, "tab_generate")
}

@Composable
fun FloatingPillTabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onToggleTheme: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = FitTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Navigation Pill
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = if (colors.isDark) 16.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = if (colors.isDark) NeonCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(colors.surfaceElevated.copy(alpha = if (colors.isDark) 0.95f else 0.98f))
                    .border(1.dp, colors.border, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primaryAccent else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.onPrimaryAccent else colors.textSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_content"
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag(tab.tag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }

            // Settings & Customization Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = if (colors.isDark) 16.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = if (colors.isDark) NeonCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(colors.surfaceElevated.copy(alpha = if (colors.isDark) 0.95f else 0.98f))
                    .border(1.dp, colors.border, CircleShape)
                    .testTag("settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
