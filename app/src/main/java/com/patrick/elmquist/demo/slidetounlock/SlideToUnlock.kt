package com.patrick.elmquist.demo.slidetounlock

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults.flingBehavior
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.patrick.elmquist.demo.slidetounlock.ui.theme.DemoSlideToUnlockTheme
import kotlin.math.roundToInt

@Composable
fun SlideToUnlock(
    isLoading: Boolean,
    onUnlockRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val swipeState = remember {
        AnchoredDraggableState(
            initialValue = if (!isLoading) Anchor.Start else Anchor.End,
            confirmValueChange = { anchor ->
                if (anchor == Anchor.End) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlockRequested()
                }
                true
            },
//            positionalThreshold = { d -> d * 1f },
//            velocityThreshold = { Float.POSITIVE_INFINITY },
//            snapAnimationSpec = tween(),
//            decayAnimationSpec = decayAnimationSpec,
        )
    }

    val swipeFraction by remember {
        derivedStateOf { calculateSwipeFraction(swipeState) }
    }

    LaunchedEffect(isLoading) {
        swipeState.animateTo(if (isLoading) Anchor.End else Anchor.Start)
    }

    Track(
        swipeState = swipeState,
        swipeFraction = swipeFraction,
        enabled = !isLoading,
        modifier = modifier,
    ) {
        Hint(
            text = "Swipe to unlock reward",
            swipeFraction = swipeFraction,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PaddingValues(horizontal = Thumb.Size + 8.dp)),
        )

        Thumb(
            isLoading = isLoading,
            modifier = Modifier.offset {
                IntOffset((swipeState.offset.takeIf { !it.isNaN() } ?: 0f).roundToInt(), 0)
            },
        )
    }
}

private fun calculateSwipeFraction(state: AnchoredDraggableState<Anchor>): Float {
    val progress = state.progress
    val atAnchor = state.settledValue == state.targetValue
    val fromStart = state.settledValue == Anchor.Start
//    val swipeFraction = if (atAnchor) {
//        if (fromStart) progress else 1f - progress
//    } else {
//        if (fromStart) progress else 1f - progress
//    }
    val swipeFraction = progress
    println("==== swipeFraction = $swipeFraction, atAnchor = $atAnchor, from = ${state.settledValue}, to = ${state.targetValue}")
    return swipeFraction
}

enum class Anchor { Start, End }

@Composable
fun Track(
    swipeState: AnchoredDraggableState<Anchor>,
    swipeFraction: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit),
) {
    val density = LocalDensity.current
    val thumbSize = Thumb.Size
    val horizontalPadding = 10.dp

    val backgroundColor by remember(swipeFraction) {
        derivedStateOf { calculateTrackColor(swipeFraction) }
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                val fullWidth = it.width
                val startOfTrackPx = 0f
                val endOfTrackPx =
                    with(density) { fullWidth - (2 * horizontalPadding + thumbSize).toPx() }
                swipeState.updateAnchors(
                    DraggableAnchors {
                        Anchor.Start at startOfTrackPx
                        Anchor.End at endOfTrackPx
                    }
                )
            }
            .height(56.dp)
            .fillMaxWidth()
            .anchoredDraggable(
                enabled = enabled,
                state = swipeState,
                orientation = Orientation.Horizontal,
                flingBehavior = flingBehavior(
                    state = swipeState,
                    positionalThreshold = { d -> d * 1f },
                ),
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(
                PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = 8.dp,
                )
            ),
        content = content,
    )
}

val AlmostBlack = Color(0xFF111111)
val Yellow = Color(0xFFFFDB00)
fun calculateTrackColor(swipeFraction: Float): Color {
    val endOfColorChangeFraction = 0.4f
    val fraction = (swipeFraction / endOfColorChangeFraction).coerceIn(0f..1f)
    return lerp(AlmostBlack, Yellow, fraction)
}

@Composable
fun Thumb(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Thumb.Size)
            .background(color = Color.White, shape = CircleShape)
            .padding(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun Hint(
    text: String,
    swipeFraction: Float,
    modifier: Modifier = Modifier,
) {
    val hintTextColor by remember(swipeFraction) {
        derivedStateOf { calculateHintTextColor(swipeFraction) }
    }

    Text(
        text = text,
        color = hintTextColor,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
    )
}

fun calculateHintTextColor(swipeFraction: Float): Color {
    val endOfFadeFraction = 0.35f
    val fraction = (swipeFraction / endOfFadeFraction).coerceIn(0f..1f)
    return lerp(Color.White, Color.White.copy(alpha = 0f), fraction)
}


private object Thumb {
    val Size = 40.dp
}

@Preview
@Composable
private fun Preview() {
    val previewBackgroundColor = Color(0xFFEDEDED)
    var isLoading by remember { mutableStateOf(false) }
    DemoSlideToUnlockTheme {
        val spacing = 88.dp
        Column(
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(previewBackgroundColor)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(spacing))

            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Normal")
                    Spacer(modifier = Modifier.weight(1f))
                    Thumb(isLoading = false)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Loading")
                    Spacer(modifier = Modifier.widthIn(min = 16.dp))
                    Thumb(isLoading = true)
                }


            }

            Spacer(modifier = Modifier.height(spacing))

            Text(text = "Inactive")
            Track(
                swipeState = AnchoredDraggableState(Anchor.Start),
                swipeFraction = 0f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Active")
            Track(
                swipeState = AnchoredDraggableState(Anchor.Start),
                swipeFraction = 1f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )


            Spacer(modifier = Modifier.height(spacing))


            SlideToUnlock(
                isLoading = isLoading,
                onUnlockRequested = { isLoading = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                shape = RoundedCornerShape(percent = 50),
                onClick = { isLoading = false }) {
                Text(text = "Cancel loading", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
