package com.gloryapps.worscanner.ui.reading

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Back
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.ended
import com.gloryapps.worscanner.ui.ExportSheet
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Segmented
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.shown
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One reading, wired to what is kept on disk and to the doors out. What it draws is `Reading`,
 * which knows neither.
 */
@Composable
internal fun ReadingScreen(stamp: String, onBack: () -> Unit, viewModel: ReadingViewModel = koinViewModel { parametersOf(stamp) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val back by rememberUpdatedState(onBack)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReadingEffect.NavigateBack -> back()
                is ReadingEffect.Copy -> context.copy(effect.label, effect.text)
            }
        }
    }

    Reading(state, viewModel::on)
    ExportSheet(viewModel.export)
}

/**
 * Every tile the scan read down one side and the card it made of it down the other, or the file
 * exactly as it was written. Where the screen is narrow the two take turns, which costs one tap to
 * reach a piece.
 */
@Composable
internal fun Reading(state: ReadingUiState, onEvent: (ReadingEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        val wide = maxWidth >= WIDE
        /* Narrow, the arrow first returns the piece to its list; the file is always one tap deep. */
        val within = !wide && state.opened && state.showing == Showing.PIECE
        BackHandler(enabled = within) { onEvent(ReadingEvent.Close) }

        Column(Modifier.fillMaxSize()) {
            Header(state, back = if (within) ReadingEvent.Close else ReadingEvent.Back, onEvent)
            Rule()

            /* What the segmented switches: the list beside it is the reading's spine and stays put. */
            val beside: @Composable (Modifier) -> Unit = {
                if (state.showing == Showing.FILE) Written(state, onEvent, it) else Detail(state, onEvent, it)
            }

            when {
                state.pieces.isEmpty() -> Written(state, onEvent, Modifier.fillMaxSize())
                wide -> Row(Modifier.fillMaxSize()) {
                    Pieces(state, onEvent, Modifier.width(LIST))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Colors.hairline))
                    beside(Modifier.weight(1f))
                }
                within || state.showing == Showing.FILE -> beside(Modifier.fillMaxSize())
                else -> Pieces(state, onEvent, Modifier.fillMaxSize())
            }
        }
    }
}

/** What the reading is called and how it ended, the two ways to see it, and the way out. */
@Composable
private fun Header(state: ReadingUiState, back: ReadingEvent, onEvent: (ReadingEvent) -> Unit) {
    val context = LocalContext.current

    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Back { onEvent(back) }
            Text(state.kept?.shown().orEmpty(), style = Lettering.subtitle, color = Colors.text, maxLines = 1)
            (state.kept as? Kept.Scan)?.let { Pill(it.ended(context)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.pieces.isNotEmpty()) {
                Segmented(
                    labels = listOf(stringResource(R.string.reading_data), stringResource(R.string.reading_json)),
                    chosen = if (state.showing == Showing.PIECE) 0 else 1,
                    onChoose = { onEvent(ReadingEvent.Show(if (it == 0) Showing.PIECE else Showing.FILE)) },
                )
            }
            if (state.kept != null) {
                Accented(
                    stringResource(R.string.reading_export),
                    said = state.pieces.size.takeIf { it > 0 }?.toString(),
                    onClick = { onEvent(ReadingEvent.Export) },
                )
            }
        }
    }
}

/** Every tile the scan read, in the order it read them. */
@Composable
internal fun Pieces(state: ReadingUiState, onEvent: (ReadingEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(pluralStringResource(R.plurals.reading_tiles, state.pieces.size, state.pieces.size), style = Lettering.caption, color = Colors.muted)
        }
        Rule()
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.pieces, key = { _, piece -> piece.index }) { at, piece ->
                if (at > 0) Rule()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (at == state.chosen) Colors.accentWash else Color.Transparent)
                        .clickable { onEvent(ReadingEvent.Choose(at)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        Modifier
                            .width(3.dp)
                            .height(46.dp)
                            .background(if (at == state.chosen) Colors.accent else Color.Transparent),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 15.dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(piece.name, style = Lettering.body, color = Colors.text, maxLines = 1)
                            Text(piece.said, style = Lettering.dataSmall, color = Colors.muted, maxLines = 1)
                        }
                        if (!piece.closed) {
                            Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(14.dp), tint = Colors.warning)
                        }
                    }
                }
            }
        }
        if (state.open > 0) {
            Rule()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(13.dp), tint = Colors.warning)
                Text(pluralStringResource(R.plurals.reading_open, state.open, state.open), style = Lettering.caption, color = Colors.muted)
            }
        }
    }
}

/** The card the reader made of the chosen tile, and where on the grid that tile was. */
@Composable
internal fun Detail(state: ReadingUiState, onEvent: (ReadingEvent) -> Unit, modifier: Modifier) {
    val piece = state.piece ?: return

    Column(modifier) {
        Above(
            name = { Text(piece.name, style = Lettering.subtitle, color = Colors.text, maxLines = 1) },
            mark = { if (!piece.closed) Pill(stringResource(R.string.reading_not_closed)) },
            said = stringResource(R.string.reading_at, piece.index, piece.row, piece.column),
            onCopy = { onEvent(ReadingEvent.Copy(piece.name, piece.card)) },
        )
        Code(piece.card)
    }
}

/** The file as it was written, which is what the lab reads, and what `copy` hands over whole where a clip can carry it. */
@Composable
internal fun Written(state: ReadingUiState, onEvent: (ReadingEvent) -> Unit, modifier: Modifier) {
    val lines = remember(state.file) { state.file.lines().size }

    Column(modifier) {
        Above(
            name = { Text(state.name, style = Lettering.data, color = Colors.text, maxLines = 1) },
            mark = { },
            said = pluralStringResource(R.plurals.reading_lines, lines, lines),
            onCopy = { onEvent(ReadingEvent.Copy(state.name, state.file)) }.takeIf { state.file.length <= COPYABLE },
        )
        Code(state.file)
    }
}

/** What sits over a block of JSON: what it is, and the one thing to do with it. */
@Composable
private fun Above(name: @Composable () -> Unit, mark: @Composable () -> Unit, said: String, onCopy: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            name()
            mark()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(said, style = Lettering.dataSmall, color = Colors.muted)
            onCopy?.let { Link(stringResource(R.string.reading_copy), onClick = it) }
        }
    }
    Rule()
}

/**
 * A JSON numbered down the side the way a file is read. Only the lines in view are coloured, so a
 * scan of a thousand pieces opens as fast as one card; the numbers stay put while the lines scroll
 * sideways under them.
 */
@Composable
private fun Code(text: String) {
    val lines = remember(text) { text.lines() }
    val across = rememberScrollState()
    val numbers = remember(lines.size) { (lines.size.toString().length * 9 + 4).dp }

    LazyColumn(
        Modifier.fillMaxSize().background(Colors.sunken),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        itemsIndexed(lines) { at, line ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${at + 1}",
                    Modifier.width(numbers),
                    style = Lettering.data,
                    color = Colors.muted,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
                Text(
                    painted(line),
                    Modifier.padding(start = 14.dp).horizontalScroll(across),
                    style = Lettering.data,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun Context.copy(label: String, text: String) =
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))

private val WIDE = 720.dp

/** Characters a clip carries safely: the clipboard hands its text to the system in one transaction of at most 1 MB. */
private const val COPYABLE = 200_000
internal val LIST = 396.dp
