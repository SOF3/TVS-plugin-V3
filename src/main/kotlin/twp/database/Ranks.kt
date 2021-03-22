package twp.database

import arc.graphics.Color
import twp.Global
import twp.database.enums.Perm
import twp.database.enums.RankType
import twp.database.enums.Stat
import twp.tools.Enums.log
import twp.tools.Json.loadHashmap
import twp.tools.Logging.info
import java.util.*

// Ranks contains all ranks available to players
class Ranks {
    // buildIn are ranks that should be always present, you still can customize them
    var buildIn = HashMap<String, Rank>()

    // special ranks can be created via specialRanks.json
    var special = HashMap<String, Rank>()

    // donation ranks can not be achieved by game progress, only by donating, thus custom ranks without quests
    // goes here
    var donation = HashMap<String, Rank>()

    // Build in ranks for nicer access as they are always present
    lateinit var griefer: Rank
    lateinit var paralyzed: Rank

    lateinit var newcomer: Rank
    lateinit var verified: Rank
    lateinit var candidate: Rank
    lateinit var admin: Rank

    // Error is placeholder - in case players rank is unresolved, It should disappear after re-log
    var error = Rank(
        true,
        false,
        "error",
        "red",
        hashMapOf("default" to "When your special rank disappears you have chance to get this super rare rank"),
        0,
        null,
        null,
        null,
        null
    )

    fun loadBuildIn() {
        griefer = Rank(
            true, false, "griefer", "#ff6bf8",
            hashMapOf("default" to "Best fit for any impostor."), 0, null, null, null, null
        )
        paralyzed = Rank(
            true,
            false,
            "paralyzed",
            "#ff9e1f",
            hashMapOf("default" to "This is placeholder in case you have to reload your account."),
            0,
            null,
            null,
            null,
            null
        )
        newcomer = Rank(
            false,
            false,
            "newcomer",
            "#b3782d",
            hashMapOf("default" to "This is first rank you will get."),
            0,
            HashSet(listOf(Perm.normal.name)),
            null,
            null,
            null
        )
        verified = Rank(
            false,
            false,
            "verified",
            "#2db3aa",
            hashMapOf("default" to "Pass the test and you ll get this. Protects your blocks against newcomers."),
            1,
            HashSet(listOf(Perm.high.name)),
            null,
            null,
            null
        )
        candidate = Rank(
            true,
            false,
            "candidate",
            "#1d991d",
            hashMapOf("default" to "This is middle step between normal player and admin."),
            2,
            HashSet(listOf(Perm.higher.name)),
            null,
            null,
            null
        )
        admin = Rank(
            true,
            true,
            "admin",
            "#2930c2",
            hashMapOf("default" to "You have power to protect others."),
            3,
            HashSet(listOf(Perm.highest.name)),
            null,
            null,
            null
        )
        buildIn[griefer.name] = griefer
        buildIn[paralyzed.name] = paralyzed
        buildIn[newcomer.name] = newcomer
        buildIn[verified.name] = verified
        buildIn[candidate.name] = candidate
        buildIn[admin.name] = admin
    }

    // if loadRanks fails it restarts everything to default because ranks can depend on each other
    // Its then easier to verify if rank links are correct
    fun loadRanks() {
        special.clear()
        donation.clear()
        val ranks = loadHashmap(rankFile, Array<Rank>::class.java, defaultRanks)
            ?: return
        val srs = ranks["ranks"] ?: return

        // To get rid of repetition
        val end = {
            special.clear()
            donation.clear()
            loadBuildIn()
            info("ranks-fileInvalid")
        }
        for (r in srs) {
            // verify permissions
            if (r.permissions != null) {
                for (p in r.permissions!!) {
                    if (log(Perm::class.java, p)) {
                        end()
                        return
                    }
                }
            }
            // quest is not important for build in rank
            if (buildIn.containsKey(r.name)) {
                buildIn[r.name] = r
                continue
            }
            // verify quest
            if (r.quests != null) {
                for (s in r.quests!!.keys) {
                    if (log(Stat::class.java, s)) {
                        end()
                        return
                    }
                    for (l in r.quests!![s]!!.keys) {
                        if (log(Rank.Mod::class.java, l)) {
                            end()
                            return
                        }
                    }
                }
            } else { // no quests so add it to donations instead
                donation[r.name] = r
                continue  // links are not important for donation ranks
            }

            // verify links
            if (r.linked != null) {
                for (l in r.linked!!) {
                    if (!ranks.containsKey(l)) {
                        info("ranks-missing", l)
                        end()
                        return
                    }
                }
            }
            special[r.name] = r
        }
    }

    fun getRank(name: String, type: RankType): Rank {
        return getRanks(type).getOrDefault(name, if (type === RankType.rank) newcomer else error)
    }

    // ranksList prints all ranks of same type
    fun rankList(type: RankType?): String {
        if (getRanks(type).isEmpty()) {
            return " none\n"
        }
        val b = StringBuilder()
        for (s in getRanks(type).values) {
            b.append(s.suffix).append(" ")
        }
        return b.substring(0, b.length - 1)
    }

    // rankType finds out a type of rank, assuming there are no duplicates
    fun rankType(name: String): RankType? {
        for (r in RankType.values()) {
            if (getRanks(r).containsKey(name)) {
                return r
            }
        }
        return null
    }

    // getRanks returns Rank group based of tipe
    fun getRanks(type: RankType?): HashMap<String, Rank> {
        return when (type) {
            RankType.rank -> buildIn
            RankType.specialRank -> special
            else -> donation
        }
    }

    companion object {
        const val rankFile = Global.config_dir + "specialRanks.json"
        var defaultRanks: HashMap<String, Array<Rank>> =
            hashMapOf("ranks" to arrayOf<Rank>(
                Rank().apply {
                    name = "kamikaze"
                    color = "scarlet"
                    description = hashMapOf(
                        "default" to "put your description here.",
                        "en_US" to "Put translation like this.",
                    )
                    value = 1
                    permissions = hashSetOf(Perm.suicide.name)
                    quests = hashMapOf(
                        Stat.deaths.name to hashMapOf(
                            Rank.Mod.best.name to 10,
                            Rank.Mod.required.name to 100,
                            Rank.Mod.frequency.name to 20,
                        )
                    )
                },
                Rank().apply {
                    name = "donor"
                    color = "#" + Color.gold.toString()
                    description = hashMapOf("default" to "For people who support server financially.")
                    permissions = hashSetOf(Perm.colorCombo.name, Perm.suicide.name)
                    pets = arrayListOf("fire-pet", "fire-pet")
                }
            ))
    }

    init {
        loadBuildIn()
        loadRanks()
    }
}