package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.kind_artifacts
import com.gloryapps.worscanner.ui.resources.kind_artifacts_note
import com.gloryapps.worscanner.ui.resources.kind_artifacts_pieces
import com.gloryapps.worscanner.ui.resources.kind_artifacts_reminder
import com.gloryapps.worscanner.ui.resources.kind_artifacts_step_1
import com.gloryapps.worscanner.ui.resources.kind_artifacts_step_2
import com.gloryapps.worscanner.ui.resources.kind_artifacts_step_3
import com.gloryapps.worscanner.ui.resources.kind_gear
import com.gloryapps.worscanner.ui.resources.kind_gear_note
import com.gloryapps.worscanner.ui.resources.kind_gear_pieces
import com.gloryapps.worscanner.ui.resources.kind_gear_reminder
import com.gloryapps.worscanner.ui.resources.kind_gear_step_1
import com.gloryapps.worscanner.ui.resources.kind_gear_step_2
import com.gloryapps.worscanner.ui.resources.kind_gear_step_3
import com.gloryapps.worscanner.ui.resources.kind_heroes
import com.gloryapps.worscanner.ui.resources.kind_heroes_note
import com.gloryapps.worscanner.ui.resources.kind_heroes_pieces
import com.gloryapps.worscanner.ui.resources.kind_heroes_reminder
import com.gloryapps.worscanner.ui.resources.kind_heroes_step_1
import com.gloryapps.worscanner.ui.resources.kind_heroes_step_2
import com.gloryapps.worscanner.ui.resources.kind_heroes_step_3
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/** The one place the apps name a kind: what it is called, and how the player readies the game for its scan. */
private class Named(val label: StringResource, val steps: List<StringResource>, val note: StringResource, val pieces: PluralStringResource, val reminder: StringResource)

private val Kind.named: Named
    get() = when (this) {
        Kind.GEAR -> Named(
            Res.string.kind_gear,
            listOf(Res.string.kind_gear_step_1, Res.string.kind_gear_step_2, Res.string.kind_gear_step_3),
            Res.string.kind_gear_note,
            Res.plurals.kind_gear_pieces,
            Res.string.kind_gear_reminder,
        )
        Kind.HEROES -> Named(
            Res.string.kind_heroes,
            listOf(Res.string.kind_heroes_step_1, Res.string.kind_heroes_step_2, Res.string.kind_heroes_step_3),
            Res.string.kind_heroes_note,
            Res.plurals.kind_heroes_pieces,
            Res.string.kind_heroes_reminder,
        )
        Kind.ARTIFACTS -> Named(
            Res.string.kind_artifacts,
            listOf(Res.string.kind_artifacts_step_1, Res.string.kind_artifacts_step_2, Res.string.kind_artifacts_step_3),
            Res.string.kind_artifacts_note,
            Res.plurals.kind_artifacts_pieces,
            Res.string.kind_artifacts_reminder,
        )
    }

/** What its button, its notification and its list entry call it. */
val Kind.label: StringResource
    get() = named.label

/** What the player does in the game before pressing Scan, in order. */
val Kind.steps: List<StringResource>
    get() = named.steps

/** What the scan does once it runs, said under the steps. */
val Kind.note: StringResource
    get() = named.note

/** The steps in one line, under the choice in the menu over the game. */
val Kind.reminder: StringResource
    get() = named.reminder

/** What one tile of it is called, counted: `pieces read`. */
val Kind.pieces: PluralStringResource
    get() = named.pieces
