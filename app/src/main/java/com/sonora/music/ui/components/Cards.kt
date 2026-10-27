package com.sonora.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonora.music.data.ArtRequest
import com.sonora.music.ui.theme.SonoraColors

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = SonoraColors.TextSecondary,
                modifier = Modifier.bounceClick(onClick = onAction),
            )
        }
    }
}

/** Square cover card used in horizontal carousels. */
@Composable
fun CoverCard(
    title: String,
    subtitle: String,
    art: ArtRequest?,
    seed: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    circle: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
) {
    Column(modifier.width(size).bounceClick(onClick = onClick)) {
        Box {
            Artwork(
                art, seed,
                Modifier.size(size),
                if (circle) CircleShape else RoundedCornerShape(6.dp),
                icon = if (circle) Icons.Rounded.Person else Icons.Rounded.MusicNote,
            )
            badge?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = if (circle) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (circle) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SonoraColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (circle) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Horizontal carousel with consistent spacing. */
@Composable
fun <T> Carousel(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items.size, key = { key(items[it]) }) { index ->
            content(items[index])
        }
    }
}

/** Colored "Mix" stripe overlay used on generated mixes. */
@Composable
fun MixBadge(color: Color, label: String, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.BottomStart) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier
                .padding(start = 10.dp, bottom = 14.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
