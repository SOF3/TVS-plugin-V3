package twp.database.core

import java.lang.StringBuilder
import twp.database.enums.Stat
import org.bson.Document

// common behavior of database document
open class Raw {
    @JvmField
    var data: Document? = null
    fun getStat(stat: Stat): Long {
        return getStat(stat.name)
    }

    fun getStat(stat: String?): Long {
        val `val` = data!![stat] as Long?
        return `val` ?: 0
    }

    val id: Long?
        get() = data!!["_id"] as Long?

    fun fieldList(): String {
        val sb = StringBuilder()
        for (s in data!!.keys) {
            sb.append(s).append(" ")
        }
        return sb.toString()
    }

    val name: String?
        get() = data!!["name"] as String?
}