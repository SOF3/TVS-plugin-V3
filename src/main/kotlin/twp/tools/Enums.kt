package twp.tools

import arc.util.Log
import java.lang.StringBuilder
import kotlin.jvm.JvmStatic
import twp.database.enums.Perm
import mindustry.gen.Player

// Enum utility functions
object Enums {
    // returns whether enum contains a value
    @JvmStatic
    fun <T : Enum<T>?> contains(cl: Class<T>, value: String): Boolean {
        for (v in cl.enumConstants) {
            if (v!!.name == value) return true
        }
        return false
    }

    // prints all enum variants
    @JvmStatic
    fun <T : Enum<T>?> list(cl: Class<T>): String {
        val s = StringBuilder()
        for (v in cl.enumConstants) {
            s.append(v).append(", ")
        }
        return s.substring(0, s.length - 2)
    }

    // combination of contains and list, this also prints available values
    fun <T : Enum<T>?> log(cl: Class<T>, value: String, p: Printer): Boolean {
        if (!contains(cl, value)) {
            p.run("Permission $value does not exist.")
            p.run("Available: " + list(Perm::class.java))
            return true
        }
        return false
    }

    fun <T : Enum<T>?> log(cl: Class<T>?, value: String?): Boolean {
        return log(cl, value, Printer { `object`: String? -> Log.info(`object`) })
    }

    fun <T : Enum<T>?> log(cl: Class<T>?, value: String?, player: Player): Boolean {
        return log(cl, value, Printer { text: String? -> player.sendMessage(text) })
    }

    interface Printer {
        fun run(message: String?)
    }
}