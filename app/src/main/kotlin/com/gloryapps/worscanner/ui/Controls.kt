package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How much room a filled action takes: in a strip over the screen, or on the screen itself. */
enum class Reach(val height: Dp, val radius: Dp, val sides: Dp, val icon: Dp, val style: TextStyle) {
    SMALL(30.dp, 7.dp, 14.dp, 16.dp, Lettering.actionSmall),
    REGULAR(44.dp, 9.dp, 22.dp, 18.dp, Lettering.action),
}

/** The one action a screen leads with: filled with the accent. `said` is the hint printed beside it; `trailing` points where it goes. */
@Composable
fun Accented(
    label: String,
    modifier: Modifier = Modifier,
    said: String? = null,
    icon: ImageVector? = null,
    trailing: ImageVector? = null,
    reach: Reach = Reach.REGULAR,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Filled(label, modifier, Colors.accent, said, icon, trailing, reach, enabled, onClick)
}

/** The action that cannot be undone: filled too, but never in the accent, which is for what a screen wants. */
@Composable
fun Destructive(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Filled(label, modifier, Colors.failure, said = null, icon = null, trailing = null, reach = Reach.REGULAR, enabled = true, onClick = onClick)
}

@Composable
private fun Filled(
    label: String,
    modifier: Modifier,
    ground: Color,
    said: String?,
    icon: ImageVector?,
    trailing: ImageVector?,
    reach: Reach,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(reach.height)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(reach.radius))
            .background(ground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = reach.sides),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, Modifier.size(reach.icon), tint = Colors.onAccent) }
        Text(label, style = reach.style, color = Colors.onAccent)
        if (said != null) Text(said, style = Lettering.dataSmall, color = Colors.onAccent.copy(alpha = 0.7f))
        trailing?.let { Icon(it, contentDescription = null, Modifier.size(reach.icon), tint = Colors.onAccent) }
    }
}

/** An action beside the leading one, drawn as an edge with nothing inside. */
@Composable
fun Edged(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Ringed(label, modifier, 40.dp, 16.dp, RoundedCornerShape(9.dp), Colors.edge, Lettering.body, Colors.text, onClick, icon = icon)
}

/** A permission's way to be given, where giving them is what the screen is for. */
@Composable
fun Grant(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Ringed(
        label, modifier.width(84.dp), 32.dp, 0.dp, RoundedCornerShape(7.dp),
        Colors.accentEdge, Lettering.actionSmall, Colors.accent, onClick,
        ground = Colors.accentWash,
    )
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

/** A short line the player types, in the data face, `hint` showing while it is empty; Done on the keyboard is `onDone`. */
@Composable
fun Field(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, onDone: () -> Unit) {
    BasicTextField(
        value,
        onValueChange,
        modifier,
        textStyle = Lettering.data.copy(color = Colors.text),
        singleLine = true,
        cursorBrush = SolidColor(Colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { field ->
            Row(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Colors.sunken)
                    .border(1.dp, Colors.edge, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) Text(hint, style = Lettering.data, color = Colors.faint) else field()
            }
        },
    )
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
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(ground)
            .border(1.dp, edge, shape)
            .let { if (onClick == null) it else it.clickable(onClick = onClick) }
            .padding(horizontal = sides),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, Modifier.size(16.dp), tint = colour) }
        Text(label, style = style, color = colour, maxLines = 1, softWrap = false)
    }
}
