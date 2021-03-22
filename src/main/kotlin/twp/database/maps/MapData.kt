package twp.database.maps

import twp.tools.Logging.log
import java.io.File
import java.nio.file.Paths
import java.lang.RuntimeException
import twp.database.core.Raw
import mindustry.maps.Map
import org.bson.Document
import org.bson.types.Binary

// Map document abstraction
class MapData(data: Document?) : Raw() {
    fun GetData(): ByteArray {
        return (data!!["data"] as Binary?)!!.data
    }

    val fileName: String
        get() = data!!.getString("fileName")
    val isEnabled: Boolean
        get() = File(Paths.get(MapHandler.Companion.mapFolder, fileName).toString()).exists()
    val rating: String
        get() {
            val ratings = data!!["rating"] as Document?
            if (ratings == null || ratings.size == 0) {
                return "none"
            }
            var total = 0
            for (r in ratings.values) {
                if (r is Int) {
                    total += r
                } else {
                    log(RuntimeException("illegal data in map ratings"))
                }
            }
            return (total / ratings.size).toString()
        }

    fun summarize(map: Map): String {
        return String.format("%d - %s - (%s/10)", id, map.name(), rating)
    }

    companion object {
        fun getNew(data: Document?): MapData? {
            return data?.let { MapData(it) }
        }
    }

    init {
        this.data = data
    }
}