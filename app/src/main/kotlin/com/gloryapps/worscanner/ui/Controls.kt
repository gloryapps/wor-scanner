package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The one action a screen leads with: filled with the accent. `said` is the hint printed beside it. */
@Composable
fun Accented(label: String, modifier: Modifier = Modifier, said: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Filled(label, modifier, Colors.accent, said, enabled, onClick)
}

/** The action that cannot be undone: filled too, but never in the accent, which is for what a screen wants. */
@Composable
fun Destructive(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Filled(label, modifier, Colors.failure, said = null, enabled = true, onClick = onClick)
}

@Composable
private fun Filled(label: String, modifier: Modifier, ground: Color, said: String?, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(ground)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lettering.action, color = Colors.onAccent)
        if (said != null) Text(said, style = Lettering.dataSmall, color = Colors.onAccent.copy(alpha = 0.7f))
    }
}

/** An action beside the leading one, drawn as an edge with nothing inside. */
@Composable
fun Edged(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Ringed(label, modifier, 40.dp, 16.dp, RoundedCornerShape(9.dp), Colors.edge, Lettering.body, Colors.text, onClick)
}

/** An action that lives inside a row of someone else's content: a grant, an export. */
@Composable
fun Inline(label: String, modifier: Modifier = Modifier, accented: Boolean = false, onClick: () -> Unit) {
    Ringed(
        label, modifier, 28.dp, 11.dp, RoundedCornerShape(7.dp),
        if (accented) Colors.accentEdge else Colors.edge,
        Lettering.caption, if (accented) Colors.accent else Colors.text, onClick,
    )
}

/** A word in a ring: a kind to scan where it can be chosen, how a scan ended where it cannot. */
@Composable
fun Pill(label: String, modifier: Modifier = Modifier, chosen: Boolean = false, onClick: (() -> Unit)? = null) {
    Ringed(
        label, modifier, 24.dp, 10.dp, CircleShape,
        if (chosen) Colors.accentEdge else Colors.hairline,
        Lettering.caption, if (chosen) Colors.accent else Colors.muted, onClick,
        ground = if (chosen) Colors.accentWash else Color.Transparent,
    )
}

/** Two or more words in a track, the shown one lifted out of it. */
@Composable
fun Segmented(labels: List<String>, chosen: Int, modifier: Modifier = Modifier, onChoose: (Int) -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.raised)
            .border(1.dp, Colors.hairline, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { at, label ->
            Row(
                Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (at == chosen) Colors.lifted else Color.Transparent)
                    .clickable { onChoose(at) }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = Lettering.caption, color = if (at == chosen) Colors.text else Colors.muted)
            }
        }
    }
}

/** An action with no edge at all, which is how a screen says "and also". */
@Composable
fun Link(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(label, modifier.clickable(onClick = onClick), style = Lettering.caption, color = Colors.accent)
}

@Composable
private fun Ringed(
    label: String,
    modifier: Modifier,
    height: Dp,
    sides: Dp,
    shape: Shape,
    edge: Color,
    style: TextStyle,
    colour: Color,
    onClick: (() -> Unit)?,
    ground: Color = Color.Transparent,
) {
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(ground)
            .border(1.dp, edge, shape)
            .let { if (onClick == null) it else it.clickable(onClick = onClick) }
            .padding(horizontal = sides),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = style, color = colour)
    }
}
