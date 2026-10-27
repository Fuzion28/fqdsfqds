package com.sonora.music.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sonora.music.data.ArtRequest
import com.sonora.music.ui.theme.SonoraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cover art with a generated gradient placeholder underneath, so items without
 * artwork still look intentional.
 */
@Composable
fun Artwork(
    art: ArtRequest?,
    seed: Any,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    icon: ImageVector = Icons.Rounded.MusicNote,
) {
    Box(modifier.clip(shape)) {
        PlaceholderArt(seed, icon, Modifier.fillMaxSize())
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun PlaceholderArt(seed: Any, icon: ImageVector, modifier: Modifier = Modifier) {
    val base = SonoraColors.accentFor(seed)
    BoxWithConstraints(
        modifier.background(
            Brush.linearGradient(listOf(base, lerpColor(base, Color.Black, 0.65f)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(maxWidth * 0.42f),
        )
    }
}

/** Gradient tile used for "Titres likés". */
@Composable
fun GradientArt(colors: List<Color>, icon: ImageVector, modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(maxWidth * 0.42f))
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)

private val colorCache = HashMap<ArtRequest, Color>()

/** Extracts a rich, dark-enough color from the artwork for backgrounds. */
@Composable
fun rememberArtColor(art: ArtRequest?, seed: Any, fallback: Color = SonoraColors.accentFor(seed)): State<Color> {
    val context = LocalContext.current
    return produceState(initialValue = art?.let { colorCache[it] } ?: fallback, art, seed) {
        if (art == null) {
            value = fallback
            return@produceState
        }
        colorCache[art]?.let { value = it; return@produceState }
        val color = withContext(Dispatchers.Default) {
            val request = ImageRequest.Builder(context)
                .data(art)
                .size(160)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult
            val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
            val soft = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
            val palette = Palette.from(soft).maximumColorCount(16).generate()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
            swatch?.let { tame(Color(it.rgb)) }
        }
        value = color ?: fallback
        if (color != null) colorCache[art] = color
    }
}

/** Keeps the color saturated but dark enough for white text on top. */
private fun tame(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt(), hsv
    )
    hsv[2] = hsv[2].coerceIn(0.35f, 0.62f)
    hsv[1] = hsv[1].coerceAtMost(0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
