package twp.database.core

import com.mongodb.client.MongoCollection
import org.bson.conversions.Bson
import com.mongodb.client.model.Updates
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import twp.database.enums.Stat
import org.bson.Document
import java.lang.IllegalStateException
import java.util.regex.Pattern

// common behavior of db handler
open class Handler(protected var data: MongoCollection<Document?>, protected var counter: MongoCollection<Document>) {
    fun idFilter(id: Long): Bson {
        return Filters.eq("_id", id)
    }

    fun delete(id: Long) {
        data.deleteOne(idFilter(id))
    }

    operator fun set(id: Long, field: String?, value: Any) {
        data.updateOne(idFilter(id), Updates.set(field, value))
    }

    fun unset(id: Long, field: String?) {
        data.updateOne(idFilter(id), Updates.unset(field))
    }

    // Pull removes value from array in database document
    fun pull(id: Long, field: String?, value: Any) {
        data.updateOne(idFilter(id), Updates.pull(field, value))
    }

    fun contains(id: Long, field: String?, value: Any): Boolean {
        return data.find(Filters.and(idFilter(id), Filters.eq(field, value))).first() != null
    }

    operator fun get(id: Long, field: String?): Any? {
        val dc = data.find(idFilter(id)).first() ?: return null
        return dc[field]
    }

    fun inc(id: Long, stat: Stat, amount: Long) {
        data.updateOne(idFilter(id), Updates.inc(stat.name, amount))
    }

    fun incOne(id: Long, stat: Stat) {
        inc(id, stat, 1)
    }

    fun getStat(id: Long, stat: String?): Long {
        val `val` = get(id, stat) as Long?
        return `val` ?: 0
    }

    fun setStat(id: Long, stat: Stat, value: Long) {
        set(id, stat.name, value)
    }

    fun gt(doc: Document, stat: String?): FindIterable<Document?> {
        return data.find(Filters.gt(stat, doc[stat]))
    }

    fun find(filter: Bson?): FindIterable<Document?> {
        return data.find(filter)
    }

    fun all(): FindIterable<Document?> {
        return data.find()
    }

    fun startsWith(field: String?, sub: String?): FindIterable<Document?> {
        val pattern = Pattern.compile("^" + Pattern.quote(sub), Pattern.CASE_INSENSITIVE)
        return data.find(Filters.regex(field, pattern))
    }

    // newID creates new incremented id
    fun newId(): Long {
        if (counter.updateOne(idFilter(0), Updates.inc("id", 1)).modifiedCount == 0L) {
            var id: Long = 0
            val latest = data.find().sort(Document("_id", -1)).first()
            if (latest != null) {
                id = latest["_id"] as Long
                if (id == -1L) {
                    id = 0
                }
            }
            counter.insertOne(Document("_id", 0).append("id", id))
        }
        val counter = counter.find().first() ?: throw IllegalStateException("Well then this is fucked.")
        return counter["id"] as Long
    }

    // For testing purposes
    fun drop() {
        data.drop()
        counter.drop()
    }
}