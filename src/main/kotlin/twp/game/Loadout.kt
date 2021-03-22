package twp.game

import twp.tools.Logging.log
import java.lang.StringBuilder
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import mindustry.Vars
import mindustry.content.Items
import mindustry.game.Team
import mindustry.type.Item
import mindustry.world.blocks.storage.CoreBlock
import org.bson.Document
import twp.Main
import java.lang.IllegalAccessException
import java.util.HashMap

class Loadout(data: MongoCollection<Document>?) {
    @JvmField
    var itemIcons: HashMap<String, String> = object : HashMap<String?, String?>() {
        init {
            put("scrap", "\uf830")
            put("copper", "\uf838")
            put("lead", "\uf837")
            put("graphite", "\uf835")
            put("coal", "\uf833")
            put("titanium", "\uf832")
            put("thorium", "\uf831")
            put("silicon", "\uf82f")
            put("plastanium", "\uf82e")
            put("phase-fabric", "\uf82d")
            put("surge-alloy", "\uf82c")
            put("spore-pod", "\uf82b")
            put("sand", "\uf834")
            put("blast-compound", "\uf82a")
            put("pyratite", "\uf829")
            put("metaglass", "\uf836")
        }
    }
    @JvmField
    var items = HashMap<String, Item>()
    var data: MongoCollection<Document>? = null
    fun itemsList(): String {
        val sb = StringBuilder()
        for (i in items.values) {
            sb.append(String.format("[#%s]%s[] ", i.color.toString(), i.name))
        }
        return sb.toString()
    }

    fun stackToString(i: Item, amount: Int): String {
        return String.format("%d%s", amount, itemIcons[i.name])
    }

    private fun storage(): Document? {
        return data!!.find().first()
    }

    fun amount(i: Item): Long {
        return storage()!!.getLong(i.name) ?: return 0
    }

    operator fun set(i: Item, amount: Long) {
        data!!.updateOne(Document(), Updates.set(i.name, amount))
    }

    fun hes(i: Item, amount: Long): Boolean {
        return amount(i) >= amount
    }

    fun inc(i: Item, amount: Long) {
        data!!.updateOne(Document(), Updates.inc(i.name, amount))
    }

    companion object {
        @JvmStatic
        fun core(): CoreBlock.CoreBuild? {
            return if (Main.testMode) null else Vars.state.teams[Team.sharded].core()
        }
    }

    init {
        if (data != null) {
            this.data = data
            if (storage() == null) {
                data.insertOne(Document().append("_id", 0))
            }
        }
        for (f in Items::class.java.fields) {
            try {
                val i = f[null] as Item
                items[i.name] = i
            } catch (e: IllegalAccessException) {
                log(e)
            }
        }
    }
}