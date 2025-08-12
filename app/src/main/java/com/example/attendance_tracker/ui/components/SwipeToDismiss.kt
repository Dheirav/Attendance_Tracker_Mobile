package com.example.attendance_tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable

enum class CustomDismissDirection {
    StartToEnd, EndToStart
}

enum class CustomDismissValue {
    Default, DismissedToEnd, DismissedToStart
}

@Composable
fun rememberCustomDismissState(
    onDismiss: (CustomDismissDirection) -> Unit = {}
): MutableState<CustomDismissValue> {
    return rememberSaveable { mutableStateOf(CustomDismissValue.Default) }
}

@Composable
fun CustomSwipeToDismiss(
    state: MutableState<CustomDismissValue>,
    modifier: Modifier = Modifier,
    background: @Composable (CustomDismissDirection) -> Unit = {},
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    val recoilDistance = 0f
    val threshold = 200f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > threshold -> {
                                state.value = CustomDismissValue.DismissedToEnd
                                offsetX = recoilDistance // recoil back
                            }
                            offsetX < -threshold -> {
                                state.value = CustomDismissValue.DismissedToStart
                                offsetX = recoilDistance // recoil back
                            }
                            else -> {
                                offsetX = recoilDistance
                                state.value = CustomDismissValue.Default
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        if (offsetX > 0) {
            background(CustomDismissDirection.StartToEnd)
        } else if (offsetX < 0) {
            background(CustomDismissDirection.EndToStart)
        }
        Box(
            modifier = Modifier.offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        ) {
            content()
        }
    }
}
