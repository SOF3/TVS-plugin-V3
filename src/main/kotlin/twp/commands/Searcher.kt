package twp.commands

import arc.func.Cons
import arc.math.Mathf
import arc.util.Strings
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import twp.database.Account
import twp.database.enums.RankType
import twp.database.enums.Stat
import org.bson.Document
import twp.Main
import twp.tools.Enums
import java.io.IOException
import java.lang.StringBuilder

// Searcher interfaces with database and allows reading public data about players for players
// though is mainly supports quick search for admins for fast actions
open class Searcher : Command() {
    // limit to haw match data command can show to prevent spam on discord
    var showLimit = 20
    override fun run(id: String, vararg args: String) {
        val sb = StringBuilder()
        if (args[0] == "online") {
            if (Main.db!!.online.isEmpty()) {
                result = Result.noOneOnline
                return
            }
            for (pd in Main.db!!.online.values) {
                sb.append(pd.account.summarize(Stat.playTime)).append("\n")
            }
            setArg(sb.toString())
            result = Result.successOnline
            return
        }
        var found: FindIterable<Document?>
        var count: Long = 0
        if (args[0].endsWith("none")) {
            found = Main.db!!.handler.all()
            count = Main.db!!.size
        } else {
            found = Main.db!!.handler.startsWith("name", args[0])
            for (ignored in found) {
                count++
            }
        }
        var stat = Stat.playTime
        if (args.size > 1) {
            val rankType = Main.ranks!!.rankType(args[1])
            if (rankType == null) {
                if (!Enums.contains(Stat::class.java, args[1])) {
                    setArg(
                        1,
                        Enums.list(Stat::class.java),
                        Main.ranks!!.rankList(RankType.rank),
                        Main.ranks!!.rankList(RankType.specialRank),
                        Main.ranks!!.rankList(RankType.donationRank)
                    )
                    result = Result.invalidSearch // ok
                    return
                }
                stat = Stat.valueOf(args[1])
            } else {
                found = found.filter(Filters.eq(rankType.name, args[1]))
                count = 0
                for (ignored in found) {
                    count++
                }
            }
        }
        found = if (args.size == 4) {
            found.sort(Sorts.descending(stat.name))
        } else {
            found.sort(Sorts.ascending(stat.name))
        }
        val slice: Slice
        if (args.size > 2) {
            try {
                slice = Slice(args[2], count.toInt())
            } catch (e: IOException) {
                result = Result.invalidSlice
                return
            }
        } else {
            slice = Slice(0, showLimit, count.toInt())
        }
        if (slice.empty()) {
            result = Result.emptySlice
            return
        }
        val finalStat = stat
        slice.forEach(found) { doc: Document? ->
            val account = Account(doc)
            sb.append(account.summarize(finalStat)).append("\n")
        }
        setArg(sb.toString(), slice.len(), count, slice.len().toFloat() / count.toFloat() * 100)
    }

    internal class Slice {
        var ends: IntArray

        constructor(start: Int, end: Int, max: Int) {
            ends = slice(start, end, max)
        }

        constructor(raw: String, max: Int) {
            val parts = raw.split("=").toTypedArray()
            if (parts.size != 2 || !Strings.canParseInt(parts[0]) || !Strings.canParseInt(parts[0])) {
                throw IOException()
            }
            ends = slice(parts[0].toInt(), parts[1].toInt(), max)
        }

        fun empty(): Boolean {
            return len() == 0
        }

        fun len(): Int {
            return Math.abs(ends[0] - ends[1])
        }

        fun forEach(arr: FindIterable<Document?>, con: Cons<Document?>) {
            var i = 0
            for (doc in arr) {
                if (i >= ends[0] && i < ends[1]) {
                    con[doc]
                }
                i++
            }
        }

        companion object {
            fun slice(start: Int, end: Int, max: Int): IntArray {
                val ends = intArrayOf(start, end)
                for (i in ends.indices) {
                    if (ends[i] < 0) {
                        ends[i] = max + ends[i] + 1
                    }
                    ends[i] = Mathf.clamp(ends[i], 0, max)
                }
                if (ends[0] > ends[1]) {
                    val temp = ends[0]
                    ends[0] = ends[1]
                    ends[1] = temp
                }
                return ends
            }
        }
    }

    companion object {
        var terminal: Searcher = object : Searcher() {
            init {
                showLimit = 100
            }
        }
        var game: Searcher = object : Searcher() {
            init {
                showLimit = 40
            }
        }
        var discord: Searcher = object : Searcher() {
            init {
                showLimit = 20
            }
        }
    }

    init {
        name = "search"
        argStruct = "<name-filter/none/online> [property] [slice] [inverted]"
        description =
            "lets you query trough tws database. Its fastest way to figure out ID. You can also compare your stats with others."
    }
}