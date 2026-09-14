package com.example.fitapp.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.fitapp.data.BodyMapping
import com.example.fitapp.ui.theme.FitTheme

enum class BodyOrientation {
    FRONT, BACK
}

/**
 * AnatomicalBodyMap:
 * Renders the authentic anatomical muscle body diagram from the SVG assets in assets/body/,
 * featuring pixel-accurate hit testing against the SVG path geometries and dynamic neon cyan highlights.
 * Seamlessly adapts between dark mode and light mode SVG diagrams.
 */
@Composable
fun AnatomicalBodyMap(
    isFront: Boolean,
    selectedMuscle: String?,
    onMuscleTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = FitTheme.colors

    LaunchedEffect(Unit) {
        BodyMapping.initialize(context)
    }

    val svgAssetPath = remember(isFront, selectedMuscle, colors.isDark) {
        BodyMapping.getSvgAssetPath(isFront, selectedMuscle, colors.isDark)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("anatomical_body_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // 724 x 1448 aspect ratio (0.5) from SVG viewBox
        val svgAspect = 724f / 1448f
        val containerAspect = if (containerHeightPx > 0) containerWidthPx / containerHeightPx else svgAspect

        val (renderedW, renderedH, offsetX, offsetY) = if (containerAspect > svgAspect) {
            // Height constrained
            val h = containerHeightPx
            val w = h * svgAspect
            val ox = (containerWidthPx - w) / 2f
            listOf(w, h, ox, 0f)
        } else {
            // Width constrained
            val w = containerWidthPx
            val h = if (svgAspect > 0) w / svgAspect else containerHeightPx
            val oy = (containerHeightPx - h) / 2f
            listOf(w, h, 0f, oy)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFront, selectedMuscle) {
                    detectTapGestures { tapOffset ->
                        val localX = tapOffset.x - offsetX
                        val localY = tapOffset.y - offsetY

                        if (renderedW > 0 && renderedH > 0 &&
                            localX in 0f..renderedW && localY in 0f..renderedH
                        ) {
                            val xInView = (localX / renderedW) * 724f
                            val svgX = xInView + (if (isFront) 0f else 724f)
                            val svgY = (localY / renderedH) * 1448f

                            val hitMuscle = BodyMapping.hitTest(isFront, svgX, svgY)
                            if (hitMuscle != null) {
                                onMuscleTapped(hitMuscle)
                            }
                        }
                    }
                }
                .testTag("anatomical_body_touch_overlay")
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/$svgAssetPath")
                    .decoderFactory(SvgDecoder.Factory())
                    .crossfade(120)
                    .build(),
                contentDescription = "Anatomical Body Map (${if (isFront) "Front" else "Back"} view)",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("anatomical_body_svg_image")
            )
        }
    }
}
