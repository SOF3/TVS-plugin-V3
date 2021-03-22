package twp.database.maps

import arc.Core
import arc.util.Time
import twp.tools.Logging.log
import twp.tools.Logging.info
import java.io.File
import java.nio.file.Paths
import java.lang.RuntimeException
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import java.io.IOException
import kotlin.Throws
import java.io.FileOutputStream
import com.mongodb.client.model.Indexes
import twp.database.core.Handler
import twp.database.enums.Stat
import mindustry.Vars
import mindustry.maps.Map
import org.bson.Document
import twp.Main
import java.nio.file.Files
import java.util.*

// MapHandler manages data about maps
class MapHandler(data: MongoCollection<Document?>, counter: MongoCollection<Document?>?) : Handler(data, counter) {
    fun invalidMaps(): Boolean {
        if (Main.testMode) {
            return false
        }
        var invalid = false
        for (m in Vars.maps.customMaps()) {
            val md = getMap(m.file.name())
            if (md == null) {
                makeNewMapData(m)
                continue
            }
            try {
                val dt = Files.readAllBytes(Paths.get(m.file.absolutePath()))
                if (!Arrays.equals(dt, md.GetData())) {
                    info("maps-duplicate", m.file.name())
                    invalid = true
                }
            } catch (e: IOException) {
                info("maps-unableToRead", m.file.name())
            }
        }
        return invalid
    }

    fun listMaps(): ArrayList<String?> {
        val res = ArrayList<String?>()
        if (Main.testMode) {
            for (i in 0..99) {
                res.add("map info$i")
            }
            return res
        }
        for (map in Vars.maps.customMaps()) {
            val md = getMap(map.file.name())
            if (md == null) {
                log(RuntimeException("map does not exist withing database but is loaded in server"))
                continue
            }
            res.add(md.summarize(map))
        }
        return res
    }

    fun addRating(id: Long, uuid: String, rating: Int) {
        set(id, "rating.$uuid", rating)
    }

    fun getMap(id: Long): MapData? {
        return MapData.Companion.getNew(data.find(idFilter(id)).first())
    }

    fun getMap(name: String): MapData? {
        return MapData.Companion.getNew(data.find(Filters.eq("fileName", name)).first())
    }

    // creates account with all settings enabled
    // newcomer rank and sets bord date
    fun makeNewMapData(map: Map): MapData? {
        val id = newId()
        data.insertOne(Document("_id", id))
        setStat(id, Stat.age, Time.millis())
        set(id, "fileName", map.file.name())
        setData(id, map)
        return getMap(id)
    }

    fun setData(id: Long, map: Map) {
        try {
            set(id, "data", Files.readAllBytes(Paths.get(map.file.absolutePath())))
        } catch (e: IOException) {
            log(e)
            log("unable to cache a map into a database")
        }
    }

    @Throws(IOException::class)
    fun withdrawMap(id: Long, dest: String?) {
        var dest = dest
        val md = getMap(id)
        if (md == null) {
            log("calling withdrawMap on map that does not exist")
            return
        }
        dest = Paths.get(dest, md.fileName).toString()
        val dt = md.GetData()
        val f = File(dest)
        if (!f.createNewFile()) {
            throw IOException("withdrawing already withdrawn map")
        }
        val fos = FileOutputStream(dest)
        fos.write(dt)
        fos.close()
    }

    @Throws(IOException::class)
    fun hideMap(id: Long) {
        val md = getMap(id)
        if (md == null) {
            log("calling hideMap on map that does not exist")
            return
        }
        if (!File(Paths.get(mapFolder, md.fileName).toString()).delete()) {
            throw IOException("map file does not exist " + Paths.get(mapFolder, md.fileName).toString())
        }
    }

    fun deleteMap(id: Long) {
        data.deleteOne(idFilter(id))
    }

    fun update(map: Map): Boolean {
        val md = getMap(map.file.name()) ?: return false
        setData(md.id!!, map)
        return true
    }

    companion object {
        var mapFolder = "config/maps/"
    }

    init {
        data.createIndex(Indexes.descending("fileName"))
        if (invalidMaps()) {
            info("maps-closingDueToInvalid")
            Core.app.exit()
        }
    }
}