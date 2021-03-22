package twp.commands

import arc.math.Mathf
import arc.util.Log
import twp.tools.Logging.info
import twp.tools.Text.formPage
import twp.tools.Logging.log
import twp.democracy.Voting
import twp.database.enums.Perm
import twp.democracy.Voting.VoteRunner
import java.lang.RuntimeException
import mindustry.Vars
import mindustry.gen.Call
import mindustry.maps.Map
import twp.Main

class MapChanger : Command() {
    var main: Voting = object : Voting(this, "main", 1, 5) {
        init {
            protection = Perm.change
        }
    }

    override fun run(id: String?, vararg args: String) {
        if (wrongOption(0, args, "change list rate")) return
        when (args[0]) {
            "list" -> {
                var page = 1
                if (args.size > 1) {
                    if (isNotInteger(args, 1)) {
                        return
                    }
                    page = args[1].toInt()
                }
                if (caller == null) {
                    info("custom", formPage(Main.db!!.maps.listMaps(), page, "maps", 30))
                } else {
                    Call.infoMessage(caller!!.player.p.con, formPage(Main.db!!.maps.listMaps(), page, "maps", 20))
                }
                result = Result.none
                return
            }
            "change" -> {
                if (checkArgCount(args.size, 2)) {
                    return
                }
                if (isNotInteger(args, 1)) {
                    return
                }
                val mid = args[1].toLong()
                val md = Main.db!!.maps.getMap(mid)
                if (md == null) {
                    result = Result.notFound
                    return
                }
                val map = Vars.maps.customMaps().find { m: Map -> m.file.name() == md.fileName }
                if (map == null) {
                    result = Result.invalidRequest
                    return
                }
                if (caller != null) {
                    main.pushSession(caller!!, VoteRunner { s: Voting.Session? -> changeMap(map) }, map.name())
                } else {
                    changeMap(map)
                }
                result = Result.none
                return
            }
            "rate" -> {
                if (Main.testMode) {
                    result = Result.rateSuccess
                    return
                }
                val md1 = Main.db!!.maps.getMap(Vars.state.map.file.name())
                if (md1 == null) {
                    log(RuntimeException("Current map is not present in database"))
                    result = Result.bug
                    return
                }
                if (isNotInteger(args, 1)) {
                    return
                }
                var rating = args[1].toInt()
                rating = Mathf.clamp(rating, 0, 10)
                if (caller != null) {
                    Main.db!!.maps.addRating(md1.id, caller!!.player.uuid, rating)
                } else {
                    Main.db!!.maps.addRating(md1.id, "owner", rating)
                }
                setArg(rating)
                result = Result.rateSuccess
            }
        }
    }

    fun changeMap(map: Map) {
        if (Main.testMode) {
            return
        }
        Log.info(Main.handler!!.handleMessage("nextmap " + map.name()))
        Log.info(Main.handler!!.handleMessage("gameover"))
    }

    companion object {
        var game = MapChanger()
        var terminal = MapChanger()
    }

    init {
        name = "map"
        argStruct = "<change/list/rate> [id/page]"
        description = "allows changing and rating maps, also can show list of maps"
    }
}