package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.client.Client
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Units as the 1.6.46 client's table holds them, but for the fields a hero's card is made of. */
private val UNITS: Map<Long, JsonObject> = mapOf(
    2224L to """{"m_Type":1,"m_Quality":5,"m_AdvancedUnlock":"0,1,222498;1,1,222406;2,3,160;3,1,222407;5,11,222450","m_OnShowFactionMember":[109,106],""" +
        """"m_HeroSkillLevelupList":[222403,222406,222407,222401,222498],"m_HeroSkillNotLevelupList":[222401,222430,222433,222499]}""",
    2001L to """{"m_Type":1,"m_Quality":3,"m_AdvancedUnlock":"1,1,200114;2,11,200140;5,11,200150;","m_OnShowFactionMember":[101],""" +
        """"m_HeroSkillLevelupList":[200118,200114],"m_HeroSkillNotLevelupList":[200117,200120]}""",
    2002L to """{"m_Type":1,"m_Quality":4,"m_AdvancedUnlock":"1,1,200213;2,3,159;","m_OnShowFactionMember":[101,106],""" +
        """"m_HeroSkillLevelupList":[200211,200213,200201],"m_HeroSkillNotLevelupList":[200201,200218]}""",
    2003L to """{"m_Type":1,"m_Quality":3,"m_AdvancedUnlock":"1,1,200315;2,3,3;","m_OnShowFactionMember":[],""" +
        """"m_HeroSkillLevelupList":[200312,200315],"m_HeroSkillNotLevelupList":[200301,200316,200330]}""",
    2901L to """{"m_Type":1,"m_Quality":3,"m_AdvancedUnlock":"1,1,290105;","m_OnShowFactionMember":[103],""" +
        """"m_HeroSkillLevelupList":[290103,290105,290101],"m_HeroSkillNotLevelupList":[290101,290130,290199]}""",
    2902L to """{"m_Type":1,"m_Quality":4,"m_AdvancedUnlock":"1,1,290205;5,11,290250;","m_OnShowFactionMember":[103],""" +
        """"m_HeroSkillLevelupList":[290203,290205,290201],"m_HeroSkillNotLevelupList":[290201,290230,290299]}""",
    2903L to """{"m_Type":1,"m_Quality":5,"m_AdvancedUnlock":"1,1,290305;2,11,290340;3,1,290307;5,11,290350","m_OnShowFactionMember":[103],""" +
        """"m_HeroSkillLevelupList":[290303,290305,290307,290301],"m_HeroSkillNotLevelupList":[290301,290330,290333,290334,290399]}""",
).mapValues { (_, row) -> Json.parseToJsonElement(row).jsonObject }
private val TEXTS = mapOf(
    "Unit_mName_2224" to "Pelagios", "Unit_mName_2001" to "Rex", "Unit_mName_2002" to "Brienne", "Unit_mName_2003" to "Nunea", "Unit_mName_2901" to "Clavicus",
    "Unit_mName_2902" to "Clavicus", "Unit_mName_2903" to "Clavicus",
    "HeroFaction_mFactionName_101" to "The Watchguard", "HeroFaction_mFactionName_103" to "The Nightmare Council",
    "HeroFaction_mFactionName_106" to "The Star Piercers", "HeroFaction_mFactionName_109" to "The Supreme Arbiters",
)
/** The 1.6.46 client, holding a few heroes. */
internal val CLIENT: Client = object : Client {
    override fun unit(id: Long) = UNITS[id]

    override fun units() = UNITS.keys.toList()

    override fun text(key: String) = TEXTS[key]
}
