package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R

/** A card on the screen. `leading` is the one the reader is meant to act on, and there is one of those. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    leading: Boolean = false,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (leading) 12.dp else 10.dp)
    Column(
        modifier
            .clip(shape)
            .background(Colors.raised)
            .border(1.dp, if (leading) Colors.accentEdge else Colors.hairline, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** A framed list: a card's edge, with rows that reach it. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Colors.raised)
            .border(1.dp, Colors.hairline, RoundedCornerShape(10.dp)),
        content = content,
    )
}

/**
 * A column that fills the height it is given, its weights sharing what is left over, and scrolls once
 * its content needs more than that, when nothing is left for the weights.
 */
@Composable
fun FillingColumn(modifier: Modifier = Modifier, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** The app's mark and name, where a screen says whose it is. */
@Composable
fun Brand(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(9.dp).background(Colors.accent, CircleShape))
        Text(stringResource(R.string.app_name), style = Lettering.brand, color = Colors.text)
    }
}

/** The line above a group, saying what the group is. */
@Composable
fun Section(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = Lettering.section, color = Colors.muted)
}

/** The number of a step, in a ring. */
@Composable
fun StepNumber(number: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(28.dp).border(1.dp, Colors.accentEdge, CircleShape), contentAlignment = Alignment.Center) {
        Text("$number", style = Lettering.numeral, color = Colors.accent)
    }
}

/** What separates one row from the next. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Colors.hairline))
}
