package twp.database

import arc.util.Time
import twp.database.Account.Companion.getNew
import twp.database.PD.Companion.makeParalyzed
import com.mongodb.client.MongoCollection
import org.bson.conversions.Bson
import com.mongodb.client.model.Updates
import twp.database.enums.RankType
import java.lang.StringBuilder
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import org.bson.Document
import twp.Main
import twp.database.core.Handler
import twp.database.enums.Setting
import twp.database.enums.Stat

// Manages data about players, mainly modifies the accounts
class AccountHandler(data: MongoCollection<Document?>, counter: MongoCollection<Document>) : Handler(data, counter) {
    internal enum class Indexed {
        uuid, ip, discordLink
    }

    // utility filter methods
    fun playerFilter(player: DBPlayer): Bson {
        return Filters.and(uuidFilter(player.uuid), ipFilter(player.ip))
    }

    fun playerOrFilter(player: DBPlayer): Bson {
        return Filters.or(uuidFilter(player.uuid), ipFilter(player.ip))
    }

    fun uuidFilter(uuid: String): Bson {
        return Filters.eq("uuid", uuid)
    }

    fun ipFilter(ip: String): Bson {
        return Filters.eq("ip", ip)
    }

    // Returns raw document holding players account
    fun getAccount(id: Long): Account? {
        return getNew(data.find(idFilter(id)).first())
    }

    fun getDocByDiscordLink(link: String): Account? {
        return getNew(data.find(Filters.eq("discordLink", link)).first())
    }

    fun setUuid(id: Long, uuid: String?) {
        set(id, "uuid", uuid!!)
    }

    fun setIp(id: Long, ip: String?) {
        set(id, "ip", ip!!)
    }

    fun addToSet(id: Long, field: String?, value: Any) {
        data.updateOne(idFilter(id), Updates.addToSet(field, value))
    }

    fun getPlace(doc: Account, stat: String?): Long {
        var res: Long = 0
        for (d in gt(doc.data!!, stat)) {
            res++
        }
        return res
    }

    fun getRank(id: Long, type: RankType): Rank? {
        val rankName = get(id, type.name) as String? ?: return null
        return Main.ranks.getRank(rankName, type)
    }

    fun removeRank(id: Long, type: RankType) {
        unset(id, type.name)
    }

    fun setRank(id: Long, rank: Rank?, type: RankType) {
        setRank(id, rank, type, null)
    }

    fun setRank(id: Long, rank: Rank?, type: RankType, comment: String?) {
        if (type === RankType.rank && comment != null) {
            Main.bot!!.log.logRankChange(id, rank!!, comment)
        }
        data.updateOne(idFilter(id), Updates.set(type.name, rank!!.name))
    }

    fun free(pd: PD) {
        val id = pd.id
        set(id, "textColor", pd.textColor!!)
        inc(id, Stat.playTime, Time.timeSinceMillis(pd.joined))
        val doc = getAccount(pd.id) ?: return
        // add setting level
        if (pd.dRank != null) set(id, RankType.donationRank.name, pd.dRank!!.name) else unset(
            id,
            RankType.donationRank.name
        )
        if (pd.spacialRank != null) set(id, RankType.specialRank.name, pd.spacialRank!!.name) else unset(
            id,
            RankType.specialRank.name
        )
    }

    // LoadData finds players account, if there is none it creates new,
    // if found account ip paralyzed it returns paralyzed data
    fun loadData(player: DBPlayer): PD {
        var doc = findData(player)
        if (doc == null) {
            doc = makeNewAccount(player.uuid, player.ip)
        } else if (doc.isParalyzed) {
            return makeParalyzed(player)
        }
        val pd = PD(player, doc!!)
        set(pd.id, "name", player.name)
        set(pd.id, "lastConnect", Time.millis())
        return pd
    }

    // findData searches for player data, it can return null if account does not exist or paralyzed account
    // if there are some account that fit at least with ip or uuid
    fun findData(player: DBPlayer): Account? {
        val cnd = data.find(playerFilter(player)).first()
        if (cnd != null) {
            return getNew(cnd)
        }
        var exists = false
        for (d in data.find(playerOrFilter(player))) {
            exists = true
            val doc = getNew(d)
            if (doc!!.isProtected) continue
            return doc
        }
        return if (!exists) {
            // TODO write related message to player, something like "no match found if you are old player pleas log in with command..."
            null
        } else getNew(Document("paralyzed", true))


        // TODO inform playe that he is paralyzed
    }

    // creates account with all settings enabled
    // newcomer rank and sets bord date
    fun makeNewAccount(uuid: String?, ip: String?): Account? {
        val id = newId()
        data.insertOne(Document("_id", id))
        for (s in Setting.values()) {
            addToSet(id, "settings", s.name)
        }
        setUuid(id, uuid)
        setIp(id, ip)
        setRank(id, Main.ranks!!.newcomer, RankType.rank, null)
        setStat(id, Stat.age, Time.millis())
        return getAccount(id)
    }

    // returns formatted string of suggested accounts that share ip or uuid with player
    fun getSuggestions(uuid: String, ip: String): String {
        val sb = StringBuilder("[yellow]")
        val fits: FindIterable<Document?> = data.find(Filters.or(uuidFilter(uuid), ipFilter(ip)))
        for (fit in fits) {
            val doc = getNew(fit)
            sb.append(doc!!.name).append("[gray] || []").append(doc.id).append("\n")
        }
        return sb.toString()
    }

    companion object {
        const val paralyzedId: Long = -1
        const val invalidId: Long = -2
    }

    init {

        // Initializing indexes
        for (i in Indexed.values()) {
            data.createIndex(Indexes.descending(i.name))
        }

        // If there isn't paralyzed already, add it
        val doc = getAccount(paralyzedId)
        if (doc == null) {
            data.insertOne(Document("_id", paralyzedId))
        }
    }
}