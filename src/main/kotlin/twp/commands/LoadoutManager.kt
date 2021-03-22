package twp.commands

import arc.struct.Seq
import arc.util.Log
import twp.game.Loadout.Companion.core
import twp.democracy.Voting
import twp.database.enums.Perm
import java.lang.StringBuilder
import twp.democracy.Voting.VoteRunner
import twp.game.Docks.Ship
import java.lang.Runnable
import kotlin.jvm.JvmStatic
import twp.database.enums.Stat
import mindustry.type.Item
import twp.Main
import java.util.HashMap

class LoadoutManager : Command() {
    var store: Voting = object : Voting(this, "store", 2, 4) {
        init {
            protection = Perm.loadout
            increase = Stat.loadoutVotes
        }
    }
    var get: Voting = object : Voting(this, "get", 2, 3) {
        init {
            protection = Perm.loadout
            increase = Stat.loadoutVotes
        }
    }

    override fun run(id: String?, vararg args: String) {
        if (wrongOption(0, args, "info get store")) return

        // handle info
        if (args.size == 1 && args[0] == "info") {
            // format page
            val sb = StringBuilder()
            if (Main.testMode || caller == null) { // testing stuff
                for (i in Main.db!!.loadout.items.values) {
                    sb.append(i.name).append(": ").append(Main.db!!.loadout.amount(i)).append("\n")
                }
                Log.info(sb.toString())
            } else {
                for (i in Main.db!!.loadout.items.values) {
                    sb.append(Main.db!!.loadout.amount(i)).append(Main.db!!.loadout.itemIcons[i.name]).append("\n")
                }
                caller!!.sendInfoMessage("l-info", sb.toString())
            }
            result = Result.none
            return
        } else if (checkArgCount(args.size, 3)) {
            return
        }
        if (isNotInteger(args, 2)) {
            return
        }
        val a = args[2].toInt()
        val items = parseItem(args[1])
        if (items.size == 0) {
            result = Result.invalidRequest
            setArg(Main.db!!.loadout.itemsList())
            return
        }
        when (args[0]) {
            "get" -> {
                if (!Main.docks!!.canUse()) { // no free ships
                    result = Result.penalty
                    return
                }
                val i = items.first()
                if (Main.db!!.loadout.amount(i) == 0L) { // useless, nothing to transport
                    result = Result.redundant
                    return
                }
                result = get.pushSession(
                    caller!!,
                    VoteRunner { session: Voting.Session? ->
                        // to prevent negative values
                        var amount = Math.min(a, Main.db!!.loadout.amount(i).toInt())
                        while (amount != 0) {
                            if (!Main.docks!!.canUse()) {
                                break
                            }

                            // take until possible and avoid negative values
                            var rAmount = amount
                            if (amount < Main.config.loadout.shipCapacity) {
                                amount = 0
                            } else {
                                rAmount = Main.config.loadout.shipCapacity
                                amount -= rAmount
                            }

                            // do transaction and formatting
                            val stack = Main.db!!.loadout.stackToString(i, rAmount)
                            val finalRAmount = rAmount
                            Main.db!!.loadout.inc(i, -rAmount.toLong())
                            Main.docks!!.use(Ship(stack + Ship.itemsToCore, label@ Runnable {
                                val core = core()
                                if (core == null) { // all cores gon, ship go bay bay
                                    Main.hud!!.sendMessage("l-shipIsLost", arrayOf(stack), 10, "red", "gray")
                                    return@label
                                }

                                // do transaction and return
                                core.items.add(i, finalRAmount)
                                Main.docks!!.use(Ship("returning", {}, Main.config.loadout.shipTravelTime))
                            }, Main.config.loadout.shipTravelTime))
                        }
                    },
                    Main.db!!.loadout.stackToString(
                        i,
                        Math.min(
                            (Main.config.shipLimit - Main.docks!!.ships.size) * Main.config.loadout.shipCapacity,
                            a
                        )
                    )
                )
            }
            "store" -> {
                val core = core()
                if (core == null) { // no core no resorces
                    result = Result.fail
                    return
                }

                // create icon list
                val s = StringBuilder()
                for (item in items) {
                    s.append(Main.db!!.loadout.itemIcons[item.name])
                }
                result = store.pushSession(caller!!, VoteRunner { session: Voting.Session? ->
                    // summarize transport results, do transaction
                    val sb = StringBuilder()
                    for (item in items) {
                        val am = Math.min(a, core.items[item])
                        sb.append(Main.db!!.loadout.stackToString(item!!, am)).append(" ")
                        core.items.remove(item, am)
                        Main.db!!.loadout.inc(item, am.toLong())
                    }
                    Main.hud!!.sendMessage("l-itemSend", arrayOf(sb.toString()), 10, "white")
                }, a.toString() + s.toString())
            }
        }
    }

    fun parseItem(raw: String): Seq<Item> {
        val items = Seq<Item>()
        if (raw == "all") {
            for (i in Main.db!!.loadout.items.values) {
                items.add(i)
            }
            return items
        }
        for (s in raw.split("/").toTypedArray()) {
            val i = Main.db!!.loadout.items[s]
            if (i != null) {
                items.add(i)
            }
        }
        return items
    }

    internal class IntPtr {
        var value = 0
    }

    companion object {
        var game = LoadoutManager()
        @JvmStatic
        fun main(args: Array<String>) {
            println(hasRepetition("aagdjoaskdka", 3))
        }

        fun hasRepetition(s: String, amount: Int): Boolean {
            val map = HashMap<Char, IntPtr>()
            for (i in 0 until s.length) {
                val ip = map.computeIfAbsent(s[i]) { k: Char? -> IntPtr() }
                ip.value++
                if (ip.value >= amount) {
                    return true
                }
            }
            return false
        }
    }

    init {
        name = "l"
        argStruct = "<get/store/info> [item] [amount]"
        description = "When your core is overflowing with resources you can store them in loadout for later withdrawal."
    }
}