package twp.database

import arc.util.Strings
import arc.util.Time
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.gen.Player
import org.bson.Document
import twp.Global
import twp.Main
import twp.database.enums.Perm
import twp.database.enums.RankType
import twp.database.enums.Setting
import twp.database.enums.Stat
import twp.database.maps.MapHandler
import twp.game.Loadout
import twp.tools.Logging.on
import twp.tools.Text.cleanName
import java.util.*

// Main database interface
class Database {
    class Connection {
        // mongo stuff
        val client: MongoClient = MongoClients.create(Main.config.db.address)
        val database: MongoDatabase = client.getDatabase(Main.config.db.name)
        val rawData: MongoCollection<Document?> = database.getCollection(Main.config.db.players)
        val rawMapData: MongoCollection<Document?> = database.getCollection(Main.config.db.maps)

        // handler has works directly with database
        val handler = AccountHandler(rawData, database.getCollection(counter))
        val maps = MapHandler(rawMapData, database.getCollection(mapCounter))
        val loadout = Loadout(database.getCollection(Main.config.loadout.name))
    }

    private lateinit var conn: Connection
    val database get() = conn.database
    val rawData get() = conn.rawData
    val handler get() = conn.handler
    val maps get() = conn.maps
    val loadout get() = conn.loadout

    // online player are here by their ids
    val online = HashMap<String, PD>()

    fun reconnect() {
        conn = Connection()
    }

    // function checks whether player can obtain any rank ON THREAD and gives him that
    fun checkAchievements(pd: PD, doc: Account) {
        for (r in Main.ranks.special.values) {
            pd.removeRank(r)
            pd.setSpecialRank(null)
        }
        Thread {
            for (rank in Main.ranks.special.values) {
                if (rank.condition(doc, pd)) {
                    if (pd.spacialRank == null || pd.spacialRank.value < rank.value) {
                        pd.setSpecialRank(rank)
                    }
                }
            }
            synchronized(pd) { pd.updateName() }
        }.start()
    }

    val size: Long
        get() = rawData.estimatedDocumentCount()

    fun hasDisabled(id: Long, perm: Perm): Boolean {
        return handler.contains(id, "settings", perm.name)
    }

    fun hasEnabled(id: Long, setting: Setting): Boolean {
        return handler.contains(id, "settings", setting.name)
    }

    fun hasMuted(id: Long, other: String?): Boolean {
        return handler.contains(id, "mutes", other!!)
    }

    fun findAccount(target: String): Account? {
        var p: PD? = null
        for (pd in online.values) {
            if (!pd.isInvalid && (pd.player.name == target || pd.player.p!!.name.equals(target, ignoreCase = true))) {
                p = pd
            }
        }
        if (p != null) {
            return p.account
        }
        return if (Strings.canParsePositiveInt(target)) {
            handler.getAccount(target.toLong())
        } else null
    }

    fun disconnectAccount(pd: PD) {
        if (pd.paralyzed) return
        if (!pd.account.isProtected) {
            handler.delete(pd.id)
        } else {
            handler.setUuid(pd.id, "why cant i just die")
            handler.setIp(pd.id, "because you are too week")
        }
    }

    //removes fake ranks and colors
    fun validateName(player: Player) {
        val originalName = player.name
        player.name = cleanName(player.name)
        if (originalName != player.name || player.name.length > Main.config.maxNameLength) {
            //name cannot be blank so then replace it with some random name
            if (player.name.replace(" ", "").isEmpty()) {
                player.name = pickFreeName()
            }
        }
    }

    fun pickFreeName(): String {
        for (n in names) {
            if (Groups.player.find { p: Player -> p.name == n } == null) {
                return n
            }
        }
        return Time.millis().toString()
    }

    fun cleanupOnlineList() {
        val iter: MutableIterator<Map.Entry<String, PD>> = Main.db!!.online.entries.iterator()
        while (iter.hasNext()) {
            val pd = iter.next().value
            if (pd.isInvalid) {
                iter.remove()
                continue
            }
            if (pd.disconnected()) {
                Main.db!!.handler!!.free(pd)
                iter.remove()
                continue
            }
        }
    }

    companion object {
        const val AFK = "[gray]<AFK>[]"
        const val counter = "counter"
        const val mapCounter = "mapCounter"
        const val subnetFile = Global.save_dir + "subnetBuns.json"
        const val cpnFile = Global.save_dir + "detectedVpn.json"

        // Random name replacements, you can add some fun ones in pr
        var names = arrayOf(
            "Steve",
            "Herold",
            "Jakub",
            "Socrates",
            "Poneklicean",
            "Orfeus",
            "Euridica",
            "Svorad",
            "Metod",
            "Ezechiel",
            "Arlong",
            "Luffy",
            "Prometeus",
            "Gerald"
        )

        fun docToString(doc: Document?): String {
            val d = Account.getNew(doc)
            return "[gray][yellow]" + d.id + "[] | " + d.name + " | []" + d.getRank(RankType.rank).suffix
        }
    }

    init {
        on(EventType.PlayerConnect::class.java) { e: EventType.PlayerConnect ->
            validateName(e.player)
            val pd = handler.loadData(DBPlayer(e.player))
            online[e.player.uuid()] = pd
            pd.updateName()
            Main.bundle.resolveBundle(pd)
            if (!pd.cannotInteract()) checkAchievements(pd, handler.getAccount(pd.id))
        }
        on(EventType.PlayerLeave::class.java) { e: EventType.PlayerLeave? -> cleanupOnlineList() }
        on(EventType.WithdrawEvent::class.java) { e: EventType.WithdrawEvent ->
            val pd = online[e.player.uuid()]
            if (pd != null) {
                handler.inc(pd.id, Stat.itemsTransported, e.amount.toLong())
            }
        }
        on(EventType.DepositEvent::class.java) { e: EventType.DepositEvent ->
            val pd = online[e.player.uuid()]
            if (pd != null) {
                handler.inc(pd.id, Stat.itemsTransported, e.amount.toLong())
            }
        }
        on(EventType.BlockBuildEndEvent::class.java) { e: EventType.BlockBuildEndEvent ->
            if (!e.unit.isPlayer || e.tile.block().buildCost / 60 < 1) return@on
            val pd = online[e.unit.player.uuid()]
            if (pd != null) {
                if (e.breaking) {
                    handler.inc(pd.id, Stat.buildingsBroken, 1)
                } else {
                    handler.inc(pd.id, Stat.buildingsBuilt, 1)
                }
            }
        }
        on(EventType.UnitDestroyEvent::class.java) { e: EventType.UnitDestroyEvent ->
            if (e.unit.isPlayer) {
                val pd = online[e.unit.player.uuid()]
                if (pd != null) {
                    handler.inc(pd.id, Stat.deaths, 1)
                }
            }
            for (p in Groups.player) {
                if (p.team() !== e.unit.team()) {
                    val pd = online[p.uuid()]
                    if (pd != null) {
                        handler.inc(pd.id, Stat.enemiesKilled, 1)
                    }
                }
            }
        }
        on(EventType.GameOverEvent::class.java) { e: EventType.GameOverEvent ->
            for (p in Groups.player) {
                val pd = online[p.uuid()] ?: continue
                if (p.team() === e.winner) {
                    handler.inc(pd.id, Stat.gamesWon, 1)
                }
                handler.inc(pd.id, Stat.gamesPlayed, 1)
            }
        }
        reconnect()
    }
}
