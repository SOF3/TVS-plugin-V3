package twp.security

import arc.struct.Seq
import arc.util.Time
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.net.Administration
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.ConstructBlock
import java.util.HashMap
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

abstract class Action(var type: Administration.ActionType?, var id: Long, var t: Tile?) {
    var b: Block? = null
    var config: Any? = null
    var team: Team? = null
    var rotation = 0
    var age = Time.millis()
    var outdated = false
    fun remove() {
        outdated = true
    }

    abstract fun undo()
    class ResolveResult {
        @JvmField
        var main: Action? = null
        @JvmField
        var optional: Action? = null
    }

    class Config(config: Any?, a: Action) : Action(a.type, a.id, a.t) {
        override fun undo() {
            t!!.build.configureAny(config)
        }

        init {
            this.config = config
        }
    }

    class Rotation(rotation: Int, a: Action) : Action(a.type, a.id, a.t) {
        override fun undo() {
            t!!.build.rotation(rotation)
        }

        init {
            this.rotation = rotation
        }
    }

    class Place(b: Block?, team: Team?, a: Action) : Action(a.type, a.id, a.t) {
        override fun undo() {
            if (team!!.core() == null) return
            team!!.core().items.add(Seq(b!!.requirements))
            Call.deconstructFinish(t, b, null)
        }

        init {
            this.b = b
            this.team = team
        }
    }

    class Break(b: Block?, config: Any?, rotation: Int, team: Team?, a: Action) : Action(a.type, a.id, a.t) {
        override fun undo() {
            if (team!!.core() == null) return
            ConstructBlock.constructed(t, b, null, rotation.toByte(), team, config)
        }

        init {
            this.b = b
            this.config = config
            this.rotation = rotation
            this.team = team
        }
    }

    class ActionTile : HashMap<Administration.ActionType?, ActionStack?>() {
        var id: Long = 0
        fun insert(rr: ResolveResult): Boolean {
            val `as` = computeIfAbsent(rr.main!!.type, Function { k: Administration.ActionType? -> ActionStack() })
            if (id != rr.main!!.id) {
                erase()
            } else if (!`as`!!.isEmpty && `as`.first()!!.type == rr.main!!.type) {
                when (rr.main!!.type) {
                    Administration.ActionType.breakBlock, Administration.ActionType.placeBlock -> if (rr.main!!.b === `as`.first()!!.b) {
                        return false
                    }
                }
            }
            id = rr.main!!.id
            if (rr.optional != null) {
                val as1 =
                    computeIfAbsent(rr.optional!!.type, Function { k: Administration.ActionType? -> ActionStack() })
                as1!!.insert(0, rr.optional)
            }
            `as`!!.insert(0, rr.main)
            return true
        }

        fun erase() {
            forEach(BiConsumer { at: Administration.ActionType?, ac: ActionStack ->
                ac.forEach(Consumer { obj: Action -> obj.remove() })
                ac.clear()
            })
        }
    }

    class ActionStack : Seq<Action?>() {
        fun execute(amount: Int) {
            val iter = iterator()
            var i = 0
            while (i < amount && iter.hasNext()) {
                val a = iter.next()
                if (!a!!.outdated) a.undo() else i--
                iter.remove()
                i++
            }
        }

        fun execute(time: Long) {
            val iter = iterator()
            while (iter.hasNext()) {
                val a = iter.next()
                if (Time.timeSinceMillis(a!!.age) > time) {
                    break
                }
                if (!a.outdated) a.undo()
                iter.remove()
            }
        }
    }

    companion object {
        @JvmField
        var actions = HashMap<Long, ActionStack>()
        var rr = ResolveResult()
        var ac: Action = object : Action(null, 0, null) {
            override fun undo() {}
        }

        @JvmStatic
        fun resolve(act: Administration.PlayerAction, id: Long): ResolveResult? {
            ac.type = act.type
            ac.id = id
            ac.t = act.tile
            rr.optional = null
            rr.main = null
            when (act.type) {
                Administration.ActionType.configure -> rr.main = Config(act.tile.build.config(), ac)
                Administration.ActionType.rotate -> rr.main = Rotation(act.tile.build.rotation(), ac)
                Administration.ActionType.placeBlock -> {
                    rr.main = Place(act.block, act.player.team(), ac)
                    val b = act.tile.block()

                    // this happens in case of boulders (as they are heavy) and if placeBlock happens on
                    // empty tile
                    if (b === Blocks.air || act.tile.build == null) {
                        if (rr.main != null) {
                            break
                        }
                        return null
                    }

                    // we cannot destroy core nor build block witch name starts with "block", if we do so
                    // client will crash of game will end
                    if (b.name.startsWith("build") || b.name.startsWith("core") ||
                        act.block.name.startsWith("build") || act.block.name.startsWith("core")
                    ) {
                        return null
                    }
                    val action: Action =
                        Break(b, act.tile.build.config(), act.tile.build.rotation(), act.player.team(), ac)
                    if (rr.main == null) {
                        rr.main = action
                    } else {
                        rr.optional = action
                    }
                }
                Administration.ActionType.breakBlock -> {
                    val b = act.tile.block()
                    if (b === Blocks.air || act.tile.build == null) {
                        if (rr.main != null) {
                            break
                        }
                        return null
                    }
                    if (b.name.startsWith("build") || b.name.startsWith("core") ||
                        act.block.name.startsWith("build") || act.block.name.startsWith("core")
                    ) {
                        return null
                    }
                    val action: Action =
                        Break(b, act.tile.build.config(), act.tile.build.rotation(), act.player.team(), ac)
                    if (rr.main == null) {
                        rr.main = action
                    } else {
                        rr.optional = action
                    }
                }
                else -> return null
            }
            return rr
        }

        @JvmStatic
        fun add(action: Action) {
            val `as` = actions.computeIfAbsent(action.id) { id: Long? -> ActionStack() }
            `as`.insert(0, action)
        }

        fun execute(id: Long, amount: Int) {
            if (actions.containsKey(id)) {
                actions[id]!!.execute(amount)
            }
        }

        @JvmStatic
        fun execute(id: Long, time: Long) {
            if (actions.containsKey(id)) {
                actions[id]!!.execute(time)
            }
        }
    }
}