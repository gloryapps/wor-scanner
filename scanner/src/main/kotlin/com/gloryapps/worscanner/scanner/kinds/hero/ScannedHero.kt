package com.gloryapps.worscanner.scanner.kinds.hero

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/** A hero as its three tabs showed it; what they did not show is null, and who the hero is belongs to the lab. */
@Serializable
data class ScannedHero(
    /** As the panel prints it, in capitals; as the game spells it where read off its memory. */
    val name: String?,
    val level: Int?,
    /** The filled slots of six, purple and gold. */
    val stars: Int?,
    /** The purple slots: the promoted stars. */
    val promotion: Int?,
    /** How many of Awakened I to V are lit. */
    val awakening: Int?,
    val skills: HeroSkills,
)

/** A hero's skill levels by where the Skills tab prints them, every place written even where empty; which skill a place is belongs to the lab. */
@Serializable
data class HeroSkills(
    @EncodeDefault val lord: SkillLevel? = null,
    @EncodeDefault val ultimate: SkillLevel? = null,
    @EncodeDefault val bonds: List<SkillLevel> = emptyList(),
    /** The icons under the labelled skills, left to right. */
    @EncodeDefault val row: List<SkillLevel> = emptyList(),
)

/** A level as the Skills tab prints it: `Lvl. 2/5` is 2, a bond not yet earned 0, and `Max Level` the top, whatever it is. */
@Serializable(with = SkillLevelSerializer::class)
sealed interface SkillLevel {
    data class Of(val level: Int) : SkillLevel

    data object Max : SkillLevel
}

/** On the wire a level is its number, or `"max"`. */
internal object SkillLevelSerializer : KSerializer<SkillLevel> {
    override val descriptor = PrimitiveSerialDescriptor("SkillLevel", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SkillLevel) = when (value) {
        is SkillLevel.Of -> encoder.encodeInt(value.level)
        SkillLevel.Max -> encoder.encodeString("max")
    }

    override fun deserialize(decoder: Decoder): SkillLevel {
        val read = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive

        return if (read.isString) SkillLevel.Max else SkillLevel.Of(read.int)
    }
}
