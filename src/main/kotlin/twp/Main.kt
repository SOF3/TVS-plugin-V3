package twp

import arc.Core
import arc.Events
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Timer
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.mod.Plugin
import twp.bundle.Bundle
import twp.commands.*
import twp.database.Database
import twp.database.PD
import twp.database.Ranks
import twp.democracy.Hud
import twp.discord.Bot
import twp.game.Docks
import twp.security.Limiter
import twp.tools.Logging

class Main : Plugin() {
    override fun registerServerCommands(handler: CommandHandler) {
        Companion.handler = handler
        RankSetter.terminal.registerTm(handler, null)
        Searcher.terminal.registerTm(handler, null)
        DBSetter.terminal.registerTm(handler, null)
        MapManager.terminal.registerTm(handler, null)
        MapChanger.terminal.registerTm(handler, null)
        Informer.general.registerTm(handler, null)
        handler.removeCommand("exit")
        handler.register("exit", "Exit the server application.") { arg: Array<String?>? ->
            Log.info("Shutting down server.")
            bot?.api?.disconnect()
            Vars.net.dispose()
            Core.app.exit()
        }
        handler.removeCommand("reloadmaps")
        handler.register("reloadmaps", "Reload all maps from disk.") { arg: Array<String?>? ->
            val beforeMaps = Vars.maps.all().size
            Vars.maps.reload()
            if (db!!.maps.invalidMaps()) {
                Logging.info("maps-notValid")
                Deferrer.terminal.run("", "close")
                return@register
            }
            if (Vars.maps.all().size > beforeMaps) {
                Logging.info("maps-reloadedCount", Vars.maps.all().size - beforeMaps)
            } else {
                Logging.info("maps-reloaded")
            }
        }
        handler.register("reload", "<bot/config/weapons>", "reloads stuff") { args: Array<String?> ->
            when (args[0]) {
                "bot" -> {
                    if (bot != null) {
                        bot!!.api.disconnect()
                    }
                    try {
                        bot = Bot(true)
                    } catch (e: RuntimeException) {
                        // error already logged
                    }
                }
                "config" -> Companion.config = Global.loadConfig()
                else -> {
                    Log.info("wrong option")
                    return@register
                }
            }
            Log.info("reloaded")
        }
        Deferrer.terminal.registerTm(handler, null)
        serverHandler = handler
    }

    //register commands that player can invoke in-game
    override fun registerClientCommands(handler: CommandHandler) {
        handler.removeCommand("vote")
        handler.removeCommand("votekick")
        Tester.game.registerGm(handler, null)
        VoteKick.game.registerGm(handler, null)
        RankSetter.game.registerGm(handler, null)
        MapChanger.game.registerGm(handler, null)
        Searcher.game.registerGm(handler, null)
        DBSetter.game.registerGm(handler, null)
        Voter.game.registerGm(handler, null)
        MapManager.game.registerGm(handler, null)
        Undoer.game.registerGm(handler, null)
        LoadoutManager.game.registerGm(handler, null)
        AccountManager.game.registerGm(handler) { self, _ ->
            when (self.result) {
                Command.Result.loginSuccess, Command.Result.success -> {
                    self.kickCaller(0)
                    self.notifyCaller()
                }
                else -> self.notifyCaller()
            }
        }
        Informer.general.registerGm(handler) { self: Command, pd: PD ->
            pd.sendInfoMessage(
                self.message,
                *self.argField
            )
        }
        handler.register("a", "test") { _: Array<String?>?, _: Any? ->
            Call.infoPopup("hello", 10f, 100, 100, 100, 100, 100)
            Call.infoPopup("hello2", 10f, 200, 200, 200, 200, 200)
        }
    }

    class TickEvent
    class Queue {
        var q = Seq<Runnable>()
        fun post(r: Runnable) {
            q.add(r)
        }

        fun run() {
            try {
                q.forEach { obj -> obj.run() }
            } catch (e: Exception) {
                Logging.log(e)
            }
            q.clear()
        }
    }

    companion object {
        lateinit var ranks: Ranks
        lateinit var db: Database
        lateinit var lim: Limiter
        lateinit var hud: Hud
        var bundle = Bundle()
        var testMode = false
        lateinit var serverHandler: CommandHandler
        var bot: Bot? = null
        lateinit var queue: Queue
        lateinit var handler: CommandHandler
        var config = Global.loadConfig()
        lateinit var docks: Docks
    }

    init {
        Logging.on(EventType.ServerLoadEvent::class.java) { e: EventType.ServerLoadEvent? ->
            ranks = Ranks()
            db = Database()
            lim = Limiter()
            bot = Bot(false)
            queue = Queue()
            docks = Docks()

            // this has to be last init
            hud = Hud()
            if (!testMode) {
                Timer.schedule({ queue.post { Events.fire(TickEvent()) } }, 0f, 1f)
            }
        }
        Logging.run(EventType.Trigger.update) { queue.run() }
    }
}
