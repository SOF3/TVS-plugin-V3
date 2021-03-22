package twp.democracy

import twp.tools.Logging.on
import twp.database.PD
import java.lang.StringBuilder
import twp.Main.TickEvent
import twp.database.enums.Setting
import mindustry.gen.Call
import twp.Main
import java.util.ArrayList

// Hud manages updating of ingame hud, it also removes disconnected players from online list
class Hud {
    var displayables = ArrayList<Displayable?>()
    fun sendMessage(message: String?, args: Array<Any?>, seconds: Int, vararg colors: String?) {
        Message.messages.add(Message(message, args, seconds, *colors))
    }

    fun update() {
        run {
            val iter = Message.messages.iterator()
            while (iter.hasNext()) {
                val message = iter.next()
                message.tick()
                if (message.counter < 1) {
                    iter.remove()
                }
            }
        }
        for (displayable in displayables) {
            displayable!!.tick()
        }
        val iter: MutableIterator<Map.Entry<String, PD>> = Main.db!!.online.entries.iterator()
        while (iter.hasNext()) {
            val pd = iter.next().value
            if (pd.isInvalid) {
                iter.remove()
                continue
            }
            if (pd.disconnected()) {
                Main.db!!.handler.free(pd)
                iter.remove()
                continue
            }
            if (!Main.db!!.hasEnabled(pd.id, Setting.hud)) {
                Call.hideHudText(pd.player.p.con)
                continue
            }
            val sb = StringBuilder()
            for (displayable in displayables) {
                sb.append(displayable!!.getMessage(pd))
            }
            for (message in Message.messages) {
                sb.append(message.getMessage(pd)).append("\n")
            }
            if (sb.length == 0) {
                Call.hideHudText(pd.player.p.con)
            } else {
                Call.setHudText(pd.player.p.con, "[#cbcbcb]" + sb.substring(0, sb.length - 1))
            }
        }
    }

    interface Displayable {
        fun getMessage(pd: PD): String
        fun tick()
    }

    internal class Message(var message: String?, var args: Array<Any?>, var counter: Int, vararg colors: String) :
        Displayable {
        var colors: Array<String>
        override fun getMessage(pd: PD): String {
            return String.format("[%s]%s[](%ds)", colors[counter % colors.size], pd.translate(message, *args), counter)
        }

        override fun tick() {
            counter--
        }

        companion object {
            var messages = ArrayList<Message>()
        }

        init {
            if (colors.size == 0) {
                this.colors = arrayOf("white")
            } else {
                this.colors = colors
            }
        }
    }

    init {
        displayables.add(Voting.Companion.processor)
        displayables.add(Main.docks)
        on(TickEvent::class.java) { e: TickEvent? -> update() }
    }
}