package twp.database

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import twp.Main
import twp.database.enums.Stat
import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

// Rank is what ppl has behind their names
open class Rank : Serializable {
    @JvmField
    var name = "noName"
    @JvmField
    var color = "red"
    var displayed = true
    var admin = false
    @JvmField
    var value = 0
    @JvmField
    var permissions: HashSet<String>? = HashSet()
    @JvmField
    var linked: HashSet<String>? = null
    @JvmField
    var description: HashMap<String, String>? = null
    @JvmField
    var quests: HashMap<String, HashMap<String, Int>>? = null
    @JvmField
    var pets: ArrayList<String>? = null

    constructor() {}

    @JsonCreator
    constructor(
        @JsonProperty("displayed") displayed: Boolean,
        @JsonProperty("admin") admin: Boolean,
        @JsonProperty("name") name: String?,
        @JsonProperty("color") color: String?,
        @JsonProperty("description") description: HashMap<String, String>?,
        @JsonProperty("value") value: Int,
        @JsonProperty("permissions") permissions: HashSet<String>?,
        @JsonProperty("linked") linked: HashSet<String>?,
        @JsonProperty("quests") quests: HashMap<String, HashMap<String, Int>>?,
        @JsonProperty("pets") pets: ArrayList<String>?
    ) {
        this.displayed = displayed
        this.admin = admin
        if (name != null) this.name = name
        if (color != null) this.color = color
        this.description = description
        this.value = value
        if (permissions != null) this.permissions = permissions
        this.linked = linked
        this.quests = quests
        this.pets = pets
    }

    @get:JsonIgnore
    val isPermanent: Boolean
        get() = quests == null && linked == null

    fun condition(tested: Account, pd: PD): Boolean {
        if (pd.hasObtained(this)) return true
        if (linked != null) {
            for (l in linked!!) {
                val other = Main.ranks!!.special[l]
                if (pd.hasObtained(other)) continue
                if (!other!!.condition(tested, pd)) return false
            }
        }
        if (quests == null) {
            return linked != null
        }
        for (stat in quests!!.keys) {
            val quest = quests!![stat]!!
            val `val` = tested.getStat(stat)
            var played = tested.getStat(Stat.playTime) / hour
            if (played == 0L) {
                played = 1
            }
            if (quest.containsKey(Mod.required.name) && `val` < quest[Mod.required.name]!!) return false
            if (quest.containsKey(Mod.frequency.name) && quest[Mod.frequency.name]!! > `val` / played) return false
            if (quest.containsKey(Mod.best.name) && Main.db!!.handler!!.getPlace(
                    tested,
                    stat
                ) > quest[Mod.best.name]!!
            ) return false
        }
        pd.addRank(this)
        return true
    }

    @get:JsonIgnore
    val suffix: String
        get() = "[$color]<$name>[]"

    @JsonIgnore
    fun suffix(): String {
        return if (displayed) suffix else ""
    }

    internal enum class Mod {
        best, required, frequency
    }

    companion object {
        const val hour = (1000 * 60 * 60).toLong()
    }
}