package com.lautarovculic.intentions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lautarovculic.intentions.ui.theme.AccentOrange
import com.lautarovculic.intentions.ui.theme.IntentionsTheme
import kotlinx.coroutines.delay

// Splash: orange "I" appears, stretches full-height, sweeps to the left half, then exits.
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntentionsTheme {
                SplashContent(onFinished = {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                })
            }
        }
    }
}

@Composable
private fun SplashContent(onFinished: () -> Unit) {
    // Four sequential phases.
    val appear = remember { Animatable(0f) }   // bar fades + grows in at centre
    val stretch = remember { Animatable(0f) }  // bar height -> full screen height
    val sweep = remember { Animatable(0f) }    // bar morphs into the left half panel
    val exit = remember { Animatable(0f) }     // left half drags off-screen to the left

    // Total ~1.98s: appear -> stretch -> sweep -> exit (+ short centred hold).
    LaunchedEffect(Unit) {
        appear.animateTo(1f, animationSpec = tween(330))
        delay(100)
        stretch.animateTo(1f, animationSpec = tween(410, easing = FastOutSlowInEasing))
        sweep.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(90)
        // Orange half drags off to the left while the version slides in to the centre.
        exit.animateTo(1f, animationSpec = tween(440, easing = FastOutSlowInEasing))
        delay(110)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barW = 28.dp.toPx()

            // height: appear, then stretch to full screen
            val appearedH = 64.dp.toPx() + (164.dp.toPx() - 64.dp.toPx()) * appear.value
            val curH = appearedH + (h - appearedH) * stretch.value
            val top = (h - curH) / 2f

            // horizontal: thin centred bar -> left half -> off-screen left
            val cx = w / 2f
            val startLeft = cx - barW / 2f
            val startRight = cx + barW / 2f
            val exitShift = cx * exit.value                       // slide left by a full half-width
            val left = startLeft * (1f - sweep.value) - exitShift // -> 0, then -> -w/2 (off-screen)
            val right = startRight + (cx - startRight) * sweep.value - exitShift // -> w/2, then -> 0

            drawRect(
                color = AccentOrange,
                topLeft = Offset(left, top),
                size = Size((right - left).coerceAtLeast(0f), curH),
                alpha = appear.value,
            )
        }

        // title + version slide in from the left, settle centred, dimmed
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-130 * (1f - exit.value)).dp) // left -> centre
                .alpha(exit.value),
        ) {
            Text(
                text = "Intentions",
                color = AccentOrange.copy(alpha = 0.78f),
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "v1.0.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
