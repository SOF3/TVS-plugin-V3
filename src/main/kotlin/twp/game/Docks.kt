package twp.game

import arc.struct.Seq
import twp.tools.Text.secToTime
import twp.democracy.Hud
import java.lang.Runnable
import twp.database.PD
import java.lang.StringBuilder
import twp.Main
import java.util.function.Consumer

class Docks : Hud.Displayable {
    @JvmField
    var ships = Seq<Ship>()
    fun use(ship: Ship) {
        Main.queue!!.post { ships.add(ship) }
        if (Main.testMode) Main.queue!!.run()
    }

    fun canUse(): Boolean {
        return ships.size < Main.config.shipLimit
    }

    override fun getMessage(pd: PD): String {
        val sb = StringBuilder()
        ships.forEach(Consumer { s: Ship -> sb.append(s.string()) })
        if (sb.length == 0) {
            return ""
        }
        sb.append("\n")
        return sb.toString()
    }

    override fun tick() {
        ships.filter { s: Ship ->
            s.time--
            if (s.time <= 0) {
                s.onDelivery.run()
                return@filter true
            }
            false
        }
    }

    class Ship(var message: String, var onDelivery: Runnable, var time: Int) {
        fun string(): String {
            return String.format("[gray]<>[]$message[gray]<>[]", secToTime(time))
        }

        companion object {
            var itemsFromCore = "<--%s<--\uf851"
            @JvmField
            var itemsToCore = "-->%s-->\uf869"
        }
    }
}