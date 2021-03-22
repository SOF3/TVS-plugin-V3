package twp.tools

import arc.math.Mathf
import kotlin.Throws
import java.lang.StringBuilder
import java.lang.Math
import java.lang.IllegalAccessException
import kotlin.jvm.JvmStatic
import java.lang.NoSuchFieldException
import twp.game.Loadout
import mindustry.Vars
import mindustry.content.Items
import mindustry.core.ContentLoader
import mindustry.type.Item
import java.lang.Exception
import java.util.ArrayList

// Text formatting utility
object Text {
    var itemIcons = intArrayOf(
        63536, 63544, 63543,
        63541, 63539, 63538,
        63537, 63535, 63534,
        63533, 63532, 63531,
        63540, 63530, 63529,
        63542
    )

    fun clean(string: String?, begin: String?, end: String?): String? {
        var string = string
        var fromBegin = 0
        var fromEnd = 0
        while (string!!.contains(begin!!)) {
            val first = string.indexOf(begin, fromBegin)
            val last = string.indexOf(end!!, fromEnd)
            if (first == -1 || last == -1) break
            if (first > last) {
                fromBegin = first + 1
                fromEnd = last + 1
            }
            string = string.substring(0, first) + string.substring(last + 1)
        }
        return string
    }

    fun cleanEmotes(string: String?): String? {
        return clean(string, "<", ">")
    }

    fun cleanColors(string: String?): String? {
        return clean(string, "[", "]")
    }

    @JvmStatic
    fun cleanName(name: String?): String {
        var name = name
        name = cleanColors(name)
        name = cleanEmotes(name)
        return name!!.replace(" ", "_")
    }

    @JvmStatic
    fun format(str: String, vararg args: Any?): String {
        return try {
            String.format(str, *args)
        } catch (e: Exception) {
            "$str\n[orange]There is a incorrect formatting in bundle. Please report this."
        }
    }

    @JvmStatic
    fun milsToTime(mils: Long): String {
        val sec = mils / 1000
        val min = sec / 60
        val hour = min / 60
        val days = hour / 24
        return String.format("%d:%02d:%02d:%02d", days % 365, hour % 24, min % 60, sec % 60)
    }

    @JvmStatic
    fun formPage(data: ArrayList<String?>, page: Int, title: String, pageSize: Int): String {
        var page = page
        val b = StringBuilder()
        val pageCount = Math.ceil((data.size / pageSize.toFloat()).toDouble()).toInt()
        page = Mathf.clamp(page, 1, pageCount) - 1
        val start = page * pageSize
        val end = Math.min(data.size, (page + 1) * pageSize)
        b.append("[orange]==").append(title.toUpperCase()).append("(").append(page + 1).append("/")
        b.append(pageCount).append(")==[]\n\n")
        for (i in start until end) {
            b.append(data[i]).append("\n")
        }
        return b.toString()
    }

    @JvmStatic
    fun secToTime(sec: Int): String {
        return String.format("%02d:%02d", sec / 60, sec % 60)
    }

    fun itemIcon(i: Item): String? {
        var idx = 0
        for (f in Items::class.java.fields) {
            try {
                println(f[null])
                println(i)
                if (f[null] == i) {
                    return itemIcons[idx] as Char.toString()
                }
            } catch (ignore: Exception) {
            }
            idx++
        }
        return null
    }

    @Throws(IllegalAccessException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        Vars.content = ContentLoader()
        Items().load()
        var idx = 0
        val l = Loadout(null)
        println(l.itemsList())
        for (f in Items::class.java.fields) {
            println(String.format("put(\"%s\", Items.%s);", (f[null] as Item).name, f.name))
            idx++
        }
    }

    fun formatInvalidField(what: String?, name: String?, command: String?): NoSuchFieldException {
        return NoSuchFieldException(
            String.format(
                "%s with name %s does not exist, use 'content %s' to view options",
                what,
                name,
                command
            )
        )
    }

    fun <T> listFields(c: Class<T>): String {
        val sb = StringBuilder()
        for (f in c.fields) {
            sb.append(f.name).append(" ")
        }
        return sb.toString()
    } /*

     */
}