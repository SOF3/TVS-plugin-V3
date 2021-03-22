package twp.security

import twp.security.Action.ActionTile
import java.lang.StringBuilder
import twp.database.enums.RankType
import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.net.Administration
import mindustry.world.Tile
import twp.Main
import java.util.ArrayList

// LockMap saves data about player actions, It is rebuild on start of each game to fit the map
class LockMap internal constructor(width: Int, height: Int) {
    var map: Array<Array<TileInf?>>
    fun getLock(t: Tile): Int {
        return map[t.y.toInt()][t.x.toInt()]!!.lock
    }

    fun setLock(t: Tile, level: Int) {
        map[t.y.toInt()][t.x.toInt()]!!.lock = level
    }

    fun displayInfo(t: Tile, p: Player) {
        Call.label(p.con, map[t.y.toInt()][t.x.toInt()]!!.format(), 10f, t.worldx(), t.worldy())
    }

    fun addAction(rr: Action.ResolveResult): Boolean {
        return map[rr.main!!.t!!.y.toInt()][rr.main!!.t!!.x.toInt()]!!.addAction(rr)
    }

    fun remove(t: Tile) {
        setLock(t, 0)
        map[t.y.toInt()][t.x.toInt()]!!.actionTile.erase()
    }

    class TileInf {
        var lock = 0
        var actions = ArrayList<ActionInf>()
        var actionTile = ActionTile()
        fun format(): String {
            val sb = StringBuilder()
            sb.append("Lock: ").append(lock).append("\n")
            for (ai in actions) {
                sb.append(ai.format()).append("\n")
            }
            return sb.substring(0, sb.length - 1)
        }

        fun addAction(rr: Action.ResolveResult): Boolean {
            val res = actionTile.insert(rr)
            when (rr.main!!.type) {
                Administration.ActionType.breakBlock, Administration.ActionType.placeBlock -> if (actions.size != 0 && actions[0].type == rr.main!!.type && actions[0].id == rr.main!!.id) {
                    return res
                }
            }
            actions.add(0, ActionInf(rr.main!!.id, rr.main!!.type))
            if (actions.size > Main.config.sec.actionMemorySize) {
                actions.removeAt(actions.size - 1)
            }
            return res
        }
    }

    class ActionInf(var id: Long, var type: Administration.ActionType?) {
        fun format(): String {
            val pd = Main.db!!.handler.getAccount(id) ?: return "hello there, i em corrupted"
            return id.toString() + "-" + pd.name + "-" + pd.getRank(RankType.rank).suffix + "-" + type!!.name
        }
    }

    init {
        map = Array(height) { arrayOfNulls(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                map[y][x] = TileInf()
            }
        }
    }
}