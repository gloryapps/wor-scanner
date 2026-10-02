package com.gloryapps.worscanner.ui.home

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.BuildConfig
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.app.provided
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Back
import com.gloryapps.worscanner.ui.Brand
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.ExportEffects
import com.gloryapps.worscanner.ui.Grants
import com.gloryapps.worscanner.ui.Inline
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.Question
import com.gloryapps.worscanner.ui.Reach
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.StepNumber
import com.gloryapps.worscanner.ui.Wide
import com.gloryapps.worscanner.ui.action
import com.gloryapps.worscanner.ui.debug.DebugLink
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.ui.note
import com.gloryapps.worscanner.ui.rememberGrant
import com.gloryapps.worscanner.ui.steps
import com.gloryapps.worscanner.update.Update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Where a scan is started, wired to the service, the system and the store. What it draws is `Home`,
 * which knows none of them.
 */
@Composable
internal fun HomeScreen(
    stage: Stage?,
    onEarlier: () -> Unit,
    onHowTo: () -> Unit,
    onDebug: () -> Unit,
    viewModel: HomeViewModel = koinViewModel { parametersOf(stage) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val earlier by rememberUpdatedState(onEarlier)
    val howTo by rememberUpdatedState(onHowTo)
    val debug by rememberUpdatedState(onDebug)

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { viewModel.left() }
    }

    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it.data
        if (it.resultCode == Activity.RESULT_OK && data != null) CaptureService.start(context, it.resultCode, data)
    }
    val grant = rememberGrant(viewModel::returned)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.Grant -> grant(effect.permission)
                HomeEffect.LaunchProjection -> projection.launch(context.getSystemService(MediaProjectionManager::class.java).wholeDisplayIntent())
                HomeEffect.StopCapture -> CaptureService.stop(context)
                HomeEffect.OpenEarlier -> earlier()
                HomeEffect.OpenHowTo -> howTo()
                HomeEffect.OpenDebug -> debug()
                /* The system's installer asks the player to confirm, and the first time to let this app install others. */
                is HomeEffect.Install -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(context.provided(effect.apk), APK_TYPE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                is HomeEffect.OpenPage -> context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
            }
        }
    }

    BackHandler(enabled = state.justScanned != null) { viewModel.on(HomeEvent.Next) }
    Home(state, viewModel::on)
    ExportEffects(viewModel.export)
}

/**
 * How a scan is made, step by step and kind by kind, beside the one action that begins it; once a scan
 * ends, that scan and the ways it leaves, until it is put away. The kinds fill the screen where it is
 * wide enough for them side by side, and stack where it is not.
 */
@Composable
internal fun Home(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        Header(state.running, scanned = state.justScanned != null, onEvent)
        /* Never beside a scan that just ended, so the update does not compete with sending it. */
        if (state.justScanned == null) (state.update as? Update.Out)?.let { Offered(it, onEvent) }
        Wide(Modifier.weight(1f)) { wide ->
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (wide) Modifier else Modifier.verticalScroll(rememberScrollState()))
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val just = state.justScanned
                if (just != null) {
                    Scanned(just, wide, onEvent, if (wide) Modifier.weight(1f) else Modifier)
                } else {
                    Steps(state, wide, onEvent)
                    Kinds(wide, if (wide) Modifier.weight(1f) else Modifier)
                }
            }
        }
    }

    if (state.asking) Ask(state.grants, onEvent)
}

/**
 * What is not on when Start is pressed, each with its way to be turned on and, under the info
 * mark, why the scanner asks. The list follows the grants as they come back from the settings, and
 * Start waits only for the ones a scan needs. `opened` is which reasons show to begin with.
 */
@Composable
internal fun Ask(grants: Grants, onEvent: (HomeEvent) -> Unit, opened: Set<Permission> = emptySet()) {
    var open by rememberSaveable { mutableStateOf(opened) }
    val missing = grants.missing

    Question(400.dp, onDismiss = { onEvent(HomeEvent.Dismiss) }) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.home_ask_title), style = Lettering.subtitle, color = Colors.text)
            Text(stringResource(R.string.home_ask_said), style = Lettering.caption, color = Colors.muted)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Colors.sunken)) {
            if (missing.isEmpty()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, Modifier.size(18.dp), tint = Colors.accent)
                    Text(stringResource(R.string.home_ask_all_on), style = Lettering.body, color = Colors.accent)
                }
            }
            missing.forEachIndexed { at, permission ->
                if (at > 0) Rule()
                Missing(
                    permission,
                    grants[permission],
                    open = permission in open,
                    onWhy = { open = if (permission in open) open - permission else open + permission },
                    onGrant = { onEvent(HomeEvent.Grant(permission)) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            Edged(stringResource(R.string.cancel), onClick = { onEvent(HomeEvent.Dismiss) })
            Accented(stringResource(R.string.home_start), enabled = grants.canScan, onClick = { onEvent(HomeEvent.Begin) })
        }
    }
}

@Composable
private fun Missing(permission: Permission, standing: Standing, open: Boolean, onWhy: () -> Unit, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(permission.icon, contentDescription = null, Modifier.size(18.dp), tint = Colors.warning)
        Text(stringResource(permission.label), Modifier.weight(1f), style = Lettering.body, color = Colors.text)
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.permission_why),
            Modifier.clip(CircleShape).clickable(onClick = onWhy).padding(6.dp).size(18.dp),
            tint = if (open) Colors.accent else Colors.muted,
        )
        standing.action?.let { Inline(stringResource(it), accented = true, onClick = onGrant) }
    }
    if (standing == Standing.STALLED) {
        Text(
            stringResource(R.string.permission_stalled),
            Modifier.padding(start = 40.dp, end = 12.dp, bottom = if (open) 4.dp else 10.dp),
            style = Lettering.caption,
            color = Colors.warning,
        )
    }
    if (open) {
        Text(
            stringResource(permission.why),
            Modifier.padding(start = 40.dp, end = 12.dp, bottom = 10.dp),
            style = Lettering.caption,
            color = Colors.muted,
        )
    }
}

/* Over a scan that just ended, its arrow puts the scan away, as back does. */
@Composable
private fun Header(running: ScanState.Running?, scanned: Boolean, onEvent: (HomeEvent) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(start = if (scanned) 0.dp else 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (scanned) Back { onEvent(HomeEvent.Next) }
                Brand()
                Text(BuildConfig.VERSION_NAME, style = Lettering.dataSmall, color = Colors.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                running?.let {
                    Text(
                        "${stringResource(it.kind.label)} ${it.progress.done}/${it.progress.held}",
                        style = Lettering.dataSmall,
                        color = Colors.muted,
                    )
                }
                HeaderLink(Icons.Outlined.PlayCircle, stringResource(R.string.howto_title)) { onEvent(HomeEvent.HowTo) }
                HeaderLink(Icons.Outlined.History, stringResource(R.string.earlier_title)) { onEvent(HomeEvent.Earlier) }
                DebugLink { onEvent(HomeEvent.Debug) }
            }
        }
        Rule()
    }
}

/** A way out of Home in its header, to a screen of its own. */
@Composable
private fun HeaderLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = Colors.muted)
        Text(label, style = Lettering.body, color = Colors.muted)
    }
}

/** A newer release across the screen under the header: what it is, and the way to it as fetching it stands. */
@Composable
private fun Offered(update: Update.Out, onEvent: (HomeEvent) -> Unit) {
    val version = update.release.version.toString()

    Column {
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(Colors.accentWash).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.SystemUpdate, contentDescription = null, Modifier.size(18.dp), tint = Colors.accent)
            Text(stringResource(R.string.home_update_out, version), style = Lettering.stepName, color = Colors.text)
            Text(stringResource(R.string.home_update_keeps), Modifier.weight(1f), style = Lettering.caption, color = Colors.muted, maxLines = 1)
            when (update) {
                is Update.Available -> Accented(stringResource(R.string.home_update_now), reach = Reach.SMALL, onClick = { onEvent(HomeEvent.Update) })
                is Update.Downloading -> Text(stringResource(R.string.home_update_downloading, version), style = Lettering.caption, color = Colors.muted)
                is Update.Failed -> Accented(stringResource(R.string.home_update_page, version), reach = Reach.SMALL, onClick = { onEvent(HomeEvent.Update) })
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Colors.accentEdge))
    }
}

/** The three steps of a scan, beside the action that takes the first: in a row where the screen is wide, one under another where it is not. */
@Composable
private fun Steps(state: HomeUiState, wide: Boolean, onEvent: (HomeEvent) -> Unit) {
    val steps: @Composable (Modifier) -> Unit = { each ->
        STEPS.forEachIndexed { at, step ->
            /* While the screen is captured, the first step has been taken and says where to go next. */
            val taken = state.capturing && at == 0
            Step(
                at + 1,
                stringResource(step.name),
                stringResource(if (taken) R.string.home_capturing else step.said),
                if (taken) Colors.accent else Colors.muted,
                each,
            )
        }
    }
    val start: @Composable (Modifier) -> Unit = {
        if (state.capturing) {
            Edged(stringResource(R.string.home_stop), it, onClick = { onEvent(HomeEvent.Stop) })
        } else {
            Accented(
                stringResource(R.string.home_start),
                it,
                said = stringResource(R.string.home_to_game),
                onClick = { onEvent(HomeEvent.Start) },
            )
        }
    }

    Card(Modifier.fillMaxWidth(), leading = true) {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) { steps(Modifier.weight(1f)) }
                start(Modifier)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { steps(Modifier.fillMaxWidth()) }
            start(Modifier.fillMaxWidth())
        }
        Rule()
        Granted(state.grants, onEvent)
    }
}

/** Every grant, always in sight under the steps: on, or its way to be turned on, or off and on. */
@Composable
private fun Granted(grants: Grants, onEvent: (HomeEvent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.firstrun_permissions), Modifier.height(28.dp).wrapContentHeight(), style = Lettering.caption, color = Colors.muted)
        Permission.entries.forEach { permission ->
            val standing = grants[permission]
            Row(Modifier.height(28.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(permission.icon, contentDescription = null, Modifier.size(16.dp), tint = if (standing == Standing.ON) Colors.accent else Colors.warning)
                Text(stringResource(permission.label), style = Lettering.caption, color = Colors.text)
                standing.action?.let { Inline(stringResource(it), accented = true, onClick = { onEvent(HomeEvent.Grant(permission)) }) }
                    ?: Text(stringResource(R.string.permission_on), style = Lettering.caption, color = Colors.accent)
            }
        }
    }
}

@Composable
private fun Step(number: Int, name: String, said: String, saidColour: Color, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        StepNumber(number)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(name, style = Lettering.stepName, color = Colors.text)
            Text(said, style = Lettering.caption, color = saidColour)
        }
    }
}

/** What to have open in the game before pressing Scan, one card per kind. */
@Composable
private fun Kinds(wide: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(stringResource(R.string.home_before))
        if (wide) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kind.entries.forEach { KindCard(it, Modifier.weight(1f).fillMaxHeight(), filling = true) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Kind.entries.forEach { KindCard(it, Modifier.fillMaxWidth(), filling = false) }
            }
        }
    }
}

/** A kind's steps, numbered, and what its scan does pinned under them; `filling` sends that note to the card's foot. */
@Composable
private fun KindCard(kind: Kind, modifier: Modifier, filling: Boolean) {
    Card(modifier, spacing = 8.dp) {
        Text(stringResource(kind.label), style = Lettering.subtitle, color = Colors.text)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            kind.steps.forEachIndexed { at, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${at + 1}", Modifier.width(8.dp).alignByBaseline(), style = Lettering.numeral, color = Colors.accent)
                    Text(stringResource(step), Modifier.alignByBaseline(), style = Lettering.body, color = Colors.text)
                }
            }
        }
        if (filling) Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Rule()
            Text(stringResource(kind.note), style = Lettering.caption, color = Colors.muted)
        }
    }
}

/** A step of every scan: what the player does, and what follows it. */
private class ScanStep(@StringRes val name: Int, @StringRes val said: Int)

private val STEPS = listOf(
    ScanStep(R.string.home_step_start, R.string.home_step_start_said),
    ScanStep(R.string.home_step_capsule, R.string.home_step_capsule_said),
    ScanStep(R.string.home_step_scan, R.string.home_step_scan_said),
)

/* Android 14 offers "one app" by default and Unity games are one app, but the scan reads the display. */
private fun MediaProjectionManager.wholeDisplayIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        createScreenCaptureIntent()
    }

private const val APK_TYPE = "application/vnd.android.package-archive"
