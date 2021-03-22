package twp.commands

import twp.tools.Logging.on
import java.lang.Runnable
import mindustry.game.EventType
import twp.Main

// sorry but no
class Deferrer : Command() {
    var initialized = false
    var deferredCall: Runnable? = null
    var reason: String? = null
    override fun run(id: String?, vararg args: String) {
        if (wrongOption(0, args, "recover stop exit")) return
        when (args[0]) {
            "recover" -> {
                deferredCall = null
                result = Result.recoverSuccess
                return
            }
            "stop", "exit" -> deferredCall = Runnable { Main.serverHandler!!.handleMessage(args[0]) }
        }
        Main.hud!!.sendMessage("deferrer-closing", arrayOfNulls(0), 30, "grey", "red")
    }

    companion object {
        var terminal = Deferrer()
    }

    init {
        name = "defer"
        argStruct = "<stop/exit/recover> [reason]"
        description = "you can deffer call of some commands, it will be called on game over"
        if (!initialized) {
            initialized = true
            on(EventType.GameOverEvent::class.java) { e: EventType.GameOverEvent? ->
                if (deferredCall != null) {
                    for (pd in Main.db!!.online.values) {
                        pd.kick("kick-custom", 0, reason)
                    }
                    deferredCall!!.run()
                }
            }
        }
    }
}