package twp.database

import arc.util.Log
import arc.util.Time
import twp.tools.Text.format
import twp.tools.Logging.info
import twp.tools.Logging.log
import twp.database.enums.Perm
import java.util.ResourceBundle
import twp.database.enums.RankType
import mindustry.gen.Call
import twp.Main
import twp.database.enums.Stat
import java.util.HashSet

// PD stores data about player that is accessed often and also handles communication with player
class PD {
    var player: DBPlayer? = null

    var textColor: String? = null

    var dRank: Rank? = null
    lateinit var rank: Rank
    lateinit var spacialRank: Rank
        private set
    private val obtained = HashSet<Rank?>()
    private val perms = HashSet<Perm>()
    var afk = false
    var paralyzed = false

    @JvmField
    var id: Long = 0
    var elapsed: Long = 0
    var lastInteraction: Long = 0
    var counter = 0
    var lastAction: Long = 0
    var lastMessage: Long = 0

    @JvmField
    var joined = Time.millis().also { lastMessage = it }.also { lastAction = it }
    private var bundle: ResourceBundle? = null
    var locString = "en_US"

    constructor()
    constructor(player: DBPlayer?, doc: Account) {
        this.player = player
        rank = doc.getRank(RankType.rank)
        textColor = doc.textColor
        id = doc.id!!
        addRank(rank)
    }

    val account: Account
        get() = Main.db.handler.getAccount(id)!!

    fun updateName() {
        if (isInvalid) {
            return
        }
        val orig = player!!.name
        val player = player!!.p
        if (afk) {
            player!!.name = "$orig[gray]<AFK>[]"
        } else if (dRank != null && dRank!!.displayed) {
            player!!.name = orig + dRank!!.suffix()
        } else if (spacialRank != null && spacialRank!!.displayed) {
            player!!.name = orig + spacialRank!!.suffix()
        } else if (rank != null) {
            player!!.name = orig + rank!!.suffix()
        }
        if (rank != null) {
            player!!.admin = rank!!.admin
        }
    }

    fun disconnected(): Boolean {
        return if (isInvalid) {
            false
        } else !player!!.p!!.con.isConnected
    }

    fun setBundle(bundle: ResourceBundle) {
        Main.db!!.handler!![id, "country"] = bundle.locale.displayCountry
        this.bundle = bundle
    }

    fun translate(key: String?, vararg args: Any?): String {
        return if (bundle != null && bundle!!.containsKey(key)) {
            format(bundle!!.getString(key), *args)
        } else format(Main.bundle.getDefault(key), *args)
    }

    fun sendServerMessage(message: String?, vararg args: Any?) {
        if (Main.testMode) {
            info(message, *args)
            return
        }
        if (isInvalid) {
            return
        }
        player!!.p!!.sendMessage(prefix + translate(message, *args))
    }

    fun sendDiscordMessage(message: String?, sender: String?) {
        if (Main.testMode) {
            info(message)
            return
        }
        if (isInvalid) {
            return
        }
        player!!.p!!.sendMessage(translate("discord-message", sender, message))
    }

    fun sendMessage(message: String?) {
        if (Main.testMode) {
            Log.info(message)
            return
        }
        if (isInvalid) {
            return
        }
        player!!.p!!.sendMessage(message)
    }

    fun sendInfoMessage(key: String?, vararg args: Any?) {
        if (Main.testMode) {
            info(key, *args)
        }
        if (isInvalid) {
            return
        }
        Call.infoMessage(player!!.p!!.con, translate(key, *args))
    }

    fun kick(message: String?, duration: Int, vararg args: Any?) {
        if (isInvalid) {
            return
        }
        player!!.p!!.con.kick(translate(message, *args), duration)
    }

    fun canParticipate(): Boolean {
        val account = account
        return !cannotInteract() && account.getStat(Stat.missedVotesCombo) < Main.config.consideredPassive && !afk
    }

    fun hasThisPerm(perm: Perm): Boolean {
        return !(paralyzed || !perms.contains(perm))
    }

    fun hasPermLevel(perm: Perm): Boolean {
        return hasPermLevel(perm.value)
    }

    fun hasPermLevel(level: Int): Boolean {
        if (paralyzed) return false
        for (p in perms) {
            if (p.value >= level) return true
        }
        return false
    }

    fun cannotInteract(): Boolean {
        return rank === Main.ranks!!.griefer || paralyzed
    }

    fun onAction() {
        lastAction = Time.millis()
        if (!afk) return
        afk = false
        updateName()
    }

    fun summarize(): String {
        return "[yellow]" + id + "[] " + player!!.name + " " + rank.getSuffix()
    }

    val isInvalid: Boolean
        get() {
            if (player!!.p == null) {
                if (!Main.testMode) log("PD has no underling player")
                return true
            }
            return false
        }
    val highestPermissionLevel: Int
        get() {
            var highest = -1
            for (p in perms) {
                if (p.value > highest) highest = p.value
            }
            return highest
        }
    val playTime: Long
        get() = Main.db!!.handler!!.getStat(id, Stat.playTime.name) + Time.timeSinceMillis(joined)

    fun addRank(rank: Rank) {
        obtained.add(rank)
        addPerms(rank)
    }

    fun addPerms(rank: Rank) {
        if (rank.permissions == null) return
        for (p in rank.permissions!!) {
            perms.add(Perm.valueOf(p))
        }
    }

    fun removeRank(rank: Rank) {
        obtained.remove(rank)
        for (s in rank.permissions!!) {
            perms.remove(Perm.valueOf(s))
        }
    }

    fun hasObtained(r: Rank): Boolean {
        return obtained.contains(r)
    }

    fun setSpecialRank(r: Rank) {
        spacialRank = r
    }

    fun actionOverflow(): Boolean {
        elapsed += Time.timeSinceMillis(lastInteraction)
        lastInteraction = Time.millis()
        if (elapsed > Main.config.sec.actionLimitFrame) {
            elapsed = 0
            counter = 0
        }
        counter++
        return counter > Main.config.sec.actionLimit
    }

    companion object {
        private const val prefix = "[coral][[[scarlet]Server[]]:[#cbcbcb] "

        @JvmStatic
        fun makeParalyzed(p: DBPlayer?): PD {
            return PD().apply {
                player = p
                paralyzed = true
                rank = Main.ranks.paralyzed
                id = AccountHandler.paralyzedId
            }
        }
    }
}