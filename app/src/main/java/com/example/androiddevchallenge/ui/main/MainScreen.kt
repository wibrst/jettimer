/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.androiddevchallenge.R
import com.example.androiddevchallenge.ui.component.CircularProgressWithThumb
import com.example.androiddevchallenge.ui.theme.JettimerTheme
import com.example.androiddevchallenge.util.ThemedPreview
import com.example.androiddevchallenge.util.TimerState
import com.example.androiddevchallenge.util.calculateFontSize
import com.example.androiddevchallenge.util.isZero
import com.example.androiddevchallenge.util.toHhMmSs
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier,
    autoPlay: Boolean,
    navigateToAdd: () -> Unit
) {
    val time = viewModel.getTimer()
    if (time.isZero()) {
        navigateToAdd()
        return
    }
    val (isFinish, setFinish) = remember { mutableStateOf(false) }
    val tick: Long by viewModel.tick.observeAsState(time)
    val timerVisibility: Boolean by viewModel.timerVisibility.observeAsState(true)
    if (autoPlay) viewModel.startTimer(time)
    val timerState: TimerState by viewModel.timerState.observeAsState(TimerState.Stopped)
    BoxWithConstraints {
        val offsetY = with(LocalDensity.current) { maxHeight.toPx().toInt() / 2 }
        AnimatedVisibility(
            visible = !isFinish,
            exit = slideOutVertically(targetOffsetY = { -offsetY }) + fadeOut(),
            enter = slideInVertically(initialOffsetY = { -offsetY }),
            initiallyVisible = false
        ) {
            MainScreenBody(
                time = time,
                tick = tick,
                modifier = modifier
                    .background(color = MaterialTheme.colors.primary)
                    .fillMaxSize(),
                timerState = timerState,
                timerVisibility = timerVisibility,
                onActionClick = { viewModel.onActionClick(timerState, time) },
                onDelete = {
                    setFinish(true)
                }
            )
        }
    }
    LaunchedEffect(isFinish) {
        if (isFinish) {
            delay(100)
            viewModel.clearTimer()
            navigateToAdd()
        }
    }
}

@Composable
fun MainScreenBody(
    time: Long,
    tick: Long,
    modifier: Modifier = Modifier,
    timerState: TimerState,
    timerVisibility: Boolean,
    onActionClick: () -> Unit,
    onDelete: () -> Unit
) {
    ConstraintLayout(modifier = modifier) {
        val (actionButtons, timer) = createRefs()
        val progress = (tick.toFloat() / time.toFloat()).coerceAtLeast(0f)
        val progressOffset = (1 - progress)
        val animatedProgress by animateFloatAsState(
            targetValue = progressOffset,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        MainTimer(
            animatedProgress = animatedProgress,
            tick = tick,
            timerState = timerState,
            timerVisibility = timerVisibility,
            modifier = Modifier
                .size(200.dp)
                .constrainAs(timer) {
                    linkTo(
                        start = parent.start,
                        top = parent.top,
                        end = parent.end,
                        bottom = actionButtons.top,
                        bottomMargin = 16.dp
                    )
                }
        )

        ActionButtons(
            onActionClick = onActionClick,
            onDelete = onDelete,
            timerState = timerState,
            modifier = Modifier
                .constrainAs(actionButtons) {
                    bottom.linkTo(parent.bottom, margin = 64.dp)
                    linkTo(
                        start = parent.start,
                        end = parent.end
                    )
                    width = Dimension.fillToConstraints
                }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainTimer(
    animatedProgress: Float,
    tick: Long,
    timerVisibility: Boolean,
    timerState: TimerState,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val progressVisibility = if (timerState == TimerState.Finished) timerVisibility else true
        if (progressVisibility) {
            CircularProgressWithThumb(
                progress = animatedProgress,
                strokeWidth = 5.dp,
                thumbSize = 7.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (label, timer, actionTimer) = createRefs()
            TextButton(
                onClick = { },
                enabled = false,
                modifier = Modifier.constrainAs(label) {
                    linkTo(start = parent.start, end = parent.end)
                    top.linkTo(parent.top, margin = 20.dp)
                }
            ) {
                Text(
                    text = stringResource(R.string.hint_label),
                    color = JettimerTheme.colors.textPrimaryColor.copy(alpha = ContentAlpha.disabled),
                    style = MaterialTheme.typography.body2.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400
                    )
                )
            }
            val labelTimerVisibility =
                if (timerState == TimerState.Paused) timerVisibility else true
            AnimatedVisibility(
                visible = labelTimerVisibility,
                enter = fadeIn(initialAlpha = 0.6f),
                exit = fadeOut(),
                modifier = Modifier.constrainAs(timer) {
                    linkTo(
                        start = parent.start,
                        top = parent.top,
                        end = parent.end,
                        bottom = parent.bottom
                    )
                }
            ) {
                val color = if (timerState == TimerState.Finished) {
                    JettimerTheme.colors.textPrimaryColor
                } else {
                    MaterialTheme.colors.secondaryVariant
                }
                val formattedTime = tick.toHhMmSs()
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.h2.copy(
                        fontSize = formattedTime.calculateFontSize(),
                        fontWeight = FontWeight.W400,
                        letterSpacing = 1.sp
                    ),
                    color = color,
                )
            }

            Button(
                onClick = { },
                elevation = null,
                modifier = Modifier.constrainAs(actionTimer) {
                    linkTo(start = parent.start, end = parent.end)
                    bottom.linkTo(parent.bottom, margin = 20.dp)
                }
            ) {
                Text(
                    text = stringResource(R.string.label_reset),
                    style = MaterialTheme.typography.body2.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400
                    ),
                    color = JettimerTheme.colors.textPrimaryColor
                )
            }
        }
    }
}

@Composable
fun ActionButtons(
    modifier: Modifier,
    timerState: TimerState,
    onActionClick: () -> Unit,
    onDelete: () -> Unit
) {
    ConstraintLayout(modifier) {
        val (action, delete, addTimer) = createRefs()
        IconButton(
            onClick = onActionClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colors.secondaryVariant,
                    shape = CircleShape
                )
                .constrainAs(action) {
                    linkTo(
                        start = parent.start,
                        end = parent.end
                    )
                }
        ) {
            val icon = when (timerState) {
                TimerState.Started -> Icons.Filled.Pause
                TimerState.Paused, TimerState.Stopped -> Icons.Outlined.PlayArrow
                TimerState.Finished -> Icons.Filled.Stop
            }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.label_start),
                tint = JettimerTheme.colors.textVariantColor
            )
        }
        Button(
            onClick = onDelete,
            elevation = null,
            modifier = Modifier.constrainAs(delete) {
                linkTo(top = action.top, bottom = action.bottom)
                start.linkTo(parent.start, margin = 16.dp)
            }
        ) {
            Text(
                text = stringResource(R.string.label_delete),
                style = MaterialTheme.typography.body2.copy(
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.W400
                ),
                color = JettimerTheme.colors.textPrimaryColor
            )
        }
        TextButton(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .constrainAs(addTimer) {
                    linkTo(top = action.top, bottom = action.bottom)
                    end.linkTo(parent.end, margin = 16.dp)
                }
        ) {
            Text(
                text = stringResource(R.string.label_add_timer),
                color = JettimerTheme.colors.textPrimaryColor.copy(alpha = ContentAlpha.disabled),
                style = MaterialTheme.typography.body2.copy(
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.W400
                )
            )
        }
    }
}

@Preview("Main screen body")
@Composable
fun PreviewHomeScreenBody() {
    ThemedPreview {
        MainScreenBody(
            time = 36000,
            tick = 3000,
            timerState = TimerState.Started,
            timerVisibility = true,
            onActionClick = {},
            onDelete = {}
        )
    }
}

@Preview("Main screen body dark")
@Composable
fun PreviewHomeScreenBodyDark() {
    ThemedPreview(darkTheme = true) {
        MainScreenBody(
            time = 36000,
            tick = 3000,
            timerState = TimerState.Started,
            timerVisibility = true,
            onActionClick = {},
            onDelete = {}
        )
    }
}
