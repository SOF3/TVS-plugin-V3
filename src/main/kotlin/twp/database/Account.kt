package twp.database

import arc.util.Time
import twp.tools.Text.milsToTime
import twp.database.enums.RankType
import org.bson.Document
import twp.Main
import twp.database.core.Raw
import twp.database.enums.Stat

// Account is player account handle, its used more to withdraw data
class Account(data: Document?) : Raw() {
    val isProtected: Boolean
        get() = password != null
    val isParalyzed: Boolean
        get() = data!!["paralyzed"] != null
    val password: Any?
        get() = data!!["password"]
    val link: String?
        get() = data!!["link"] as String?
    val ip: String?
        get() = data!!["ip"] as String?
    val textColor: String?
        get() = data!!["textColor"] as String?
    val uuid: String?
        get() = data!!["uuid"] as String?

    fun admin(): Boolean {
        return getRank(RankType.rank).admin
    }

    val latestActivity: Long
        get() = data!!.getLong("lastConnect") ?: return 0

    fun getRank(type: RankType): Rank {
        val rankName = data!![type.name] as String?
        if (rankName == null && type === RankType.rank) {
            // there is some corruption going on so this is needed
            Main.db!!.handler!!.setRank(id!!, Main.ranks!!.newcomer, RankType.rank)
            return Main.ranks!!.newcomer
        }
        return Main.ranks!!.getRank(rankName, type)
    }

    val isGriefer: Boolean
        get() = getRank(RankType.rank) === Main.ranks!!.griefer

    fun markable(): Boolean {
        return getRank(RankType.rank) !== Main.ranks!!.candidate && getRank(RankType.rank) !== Main.ranks!!.admin
    }

    fun summarize(stat: Stat): String {
        return String.format(
            "[gray]ID: [yellow]%d[] NAME: [white]%s[] RANK: %s %s: [orange]%d[] ",
            id,
            name,
            getRank(RankType.rank).suffix,
            stat.name.toUpperCase(),
            getStat(stat)
        )
    }

    fun basicStats(): Array<Any?> {
        val os = arrayOf(
            id,
            name,
            getRank(RankType.rank).suffix,
            null,
            null,
            data!!["country"],
            milsToTime(Time.timeSinceMillis(latestActivity))
        )
        for (i in 1 until RankType.values().size) {
            val s = getRank(RankType.values()[i])
            if (s !== Main.ranks!!.error) {
                os[i + 2] = s.suffix
            } else {
                os[i + 2] = "none"
            }
        }
        return os
    }

    fun stats(): Array<Any?> {
        val os = arrayOfNulls<Any>(Stat.values().size)
        for (i in Stat.values().indices) {
            val s = Stat.values()[i]
            if (s === Stat.age) {
                os[i] = milsToTime(Time.timeSinceMillis(getStat(s)))
                continue
            }
            if (s.time) {
                os[i] = milsToTime(getStat(s))
            } else {
                os[i] = getStat(s)
            }
        }
        return os
    }

    companion object {
        @JvmStatic
        fun getNew(document: Document?): Account? {
            return document?.let { Account(it) }
        }
    }

    init {
        this.data = data
    }
}