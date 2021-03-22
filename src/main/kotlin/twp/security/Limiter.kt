package twp.security

import arc.util.Time
import arc.util.Timer
import twp.security.Action.Companion.resolve
import twp.security.Action.Companion.add
import twp.security.Action.Companion.execute
import twp.database.enums.Perm
import twp.commands.RankSetter
import twp.tools.Logging
import mindustry.Vars
import mindustry.game.EventType
import mindustry.net.Administration
import mindustry.world.Tile
import twp.Main
import java.lang.Exception
import java.util.HashMap

// Limiter restricts player actions and manages instance of LockMap
class Limiter {
    var map: LockMap? = null
    var doubleClicks = HashMap<String, DoubleClickData>()
    fun registerActionFilter() {
        Vars.netServer.admins.addActionFilter { act: Administration.PlayerAction ->
            try {
                val player = act.player
                    ?: return@addActionFilter true // Dont forget this true is important
                val pd = Main.db!!.online[player.uuid()]
                if (pd == null) {
                    Logging.log("player data is missing ewen though player is attempting actions")
                    return@addActionFilter true
                }
                if (pd.rank === Main.ranks!!.griefer) {
                    pd.sendServerMessage("admins-grieferCannotBuild")
                    return@addActionFilter false
                }
                if (pd.paralyzed) {
                    pd.sendServerMessage("admins-paralyzedCannotBuild")
                    return@addActionFilter false
                }
                if (act.tile == null) {
                    return@addActionFilter true
                }
                val top = pd.highestPermissionLevel
                val lock = map!!.getLock(act.tile)
                if (lock > top) {
                    pd.sendServerMessage("admins-permissionTooLow", top, lock)
                    return@addActionFilter false
                } else if (act.type != Administration.ActionType.breakBlock && pd.hasPermLevel(Perm.high.value)) {
                    map!!.setLock(act.tile, Perm.high.value)
                }
                val rr = resolve(act, pd.id)
                if (rr != null && map!!.addAction(rr)) {
                    if (rr.optional != null) add(rr.optional!!)
                    add(rr.main!!)
                    if (act.type != Administration.ActionType.breakBlock && act.type != Administration.ActionType.placeBlock) {
                        if (pd.actionOverflow()) {
                            RankSetter.terminal.run("", pd.id.toString(), "griefer")
                            Timer.schedule({
                                Main.queue!!.post {
                                    execute(
                                        pd.id,
                                        Main.config.sec.actionUndoTime + 2000
                                    )
                                }
                            }, 2f)
                        }
                    }
                }
            } catch (e: Exception) {
                Logging.log(e)
            }
            true
        }
    }

    class DoubleClickData(var t: Tile) {
        var time = Time.millis()
        fun Equal(t: Tile): Boolean {
            return this.t === t
        }
    }

    init {
        // Initializing LockMap on start of a game
        Logging.on(EventType.PlayEvent::class.java) { e: EventType.PlayEvent? ->
            Action.actions.clear()
            map = LockMap(Vars.world.width(), Vars.world.height())
        }

        // Cases when lock should reset
        Logging.on(EventType.BlockDestroyEvent::class.java) { e: EventType.BlockDestroyEvent -> map!!.remove(e.tile) }
        Logging.on(EventType.BlockBuildEndEvent::class.java) { e: EventType.BlockBuildEndEvent ->
            if (e.breaking) {
                map!!.setLock(e.tile, 0)
            }
        }

        // This mostly prevents griefers from shooting
        Logging.run(EventType.Trigger.update) {
            for (pd in Main.db!!.online.values) {
                if (pd.isInvalid) {
                    return@run
                }
                if (pd.cannotInteract() && pd.player.p.shooting) {
                    pd.player.p.unit().kill()
                }
            }
        }
        Logging.on(EventType.TapEvent::class.java) { e: EventType.TapEvent ->
            val dcd = doubleClicks[e.player.uuid()]
            if (dcd == null || !dcd.Equal(e.tile)) {
                doubleClicks[e.player.uuid()] = DoubleClickData(e.tile)
                return@on
            }
            if (Time.timeSinceMillis(dcd.time) < Main.config.doubleClickSpacing) {
                map!!.displayInfo(e.tile, e.player)
            }
            doubleClicks.remove(e.player.uuid())
        }
        if (!Main.testMode) {
            registerActionFilter()
            Administration.Config.antiSpam.set(false)
        }
    }
}