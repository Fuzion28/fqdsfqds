package com.sonora.music.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.enterAnimation
import com.sonora.music.ui.theme.SonoraColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PermissionScreen(permission: String, onGranted: () -> Unit) {
    val context = LocalContext.current
    var denials by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[permission] == true) onGranted() else denials++
    }

    Box(Modifier.fillMaxSize().background(SonoraColors.Background)) {
        AuroraBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Logo(Modifier.enterAnimation(0))
            Spacer(Modifier.height(28.dp))
            Text(
                "Toute votre musique.\nMagnifiquement.",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.enterAnimation(1),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Sonora a besoin d'accéder aux fichiers audio de votre appareil pour construire votre bibliothèque.",
                style = MaterialTheme.typography.bodyLarge,
                color = SonoraColors.TextSecondary,
                modifier = Modifier.enterAnimation(2),
            )
            Spacer(Modifier.height(36.dp))
            Box(
                Modifier
                    .enterAnimation(3)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(SonoraColors.Green, RoundedCornerShape(50))
                    .bounceClick {
                        if (denials >= 2) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        } else {
                            val perms = buildList {
                                add(permission)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            launcher.launch(perms.toTypedArray())
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (denials >= 2) "Ouvrir les paramètres" else "Autoriser l'accès",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Vos fichiers restent sur votre appareil.",
                style = MaterialTheme.typography.bodySmall,
                color = SonoraColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().enterAnimation(4),
            )
        }
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "logo")
    val phase by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "phase")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(52.dp)
                .background(SonoraColors.Green, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(26.dp)) {
                val bars = 5
                val gap = size.width * 0.08f
                val w = (size.width - gap * (bars - 1)) / bars
                repeat(bars) { i ->
                    val h = size.height * (0.35f + 0.65f * ((sin(phase + i * 0.9f) + 1f) / 2f))
                    drawRoundRect(
                        Color.Black,
                        Offset(i * (w + gap), (size.height - h) / 2),
                        Size(w, h),
                        CornerRadius(w / 2),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text("Sonora", style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

/** Slowly drifting colored blobs. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "aurora")
    val angle by t.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val blobs = listOf(SonoraColors.Green, Color(0xFF8D67AB), Color(0xFF0D73EC), Color(0xFFE8115B))
    Canvas(modifier) {
        blobs.forEachIndexed { i, color ->
            val a = angle + i * (PI / 2).toFloat()
            val cx = size.width * (0.5f + 0.32f * cos(a * (if (i % 2 == 0) 1f else -1f)))
            val cy = size.height * (0.3f + 0.18f * sin(a * 1.3f + i))
            val r = size.width * 0.75f
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), Offset(cx, cy), r),
                radius = r,
                center = Offset(cx, cy),
            )
        }
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to SonoraColors.Background.copy(alpha = 0.6f),
                0.8f to SonoraColors.Background,
            )
        )
    }
}
