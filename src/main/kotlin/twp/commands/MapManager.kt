package twp.commands

import arc.files.Fi
import twp.tools.Logging.log
import java.io.File
import java.io.IOException
import twp.database.maps.MapHandler
import mindustry.Vars
import mindustry.io.MapIO
import twp.Main

open class MapManager : Command() {
    override fun run(id: String?, vararg args: String) {
        if (!verifier.verify(id)) {
            result = Result.noPerm
            return
        }
        if (wrongOption(0, args, "enable disable add remove update")) return
        if (args[0] == "add" || args[0] == "update") {
            if (!File(args[1]).exists()) {
                result = Result.notExist
                return
            }
            try {
                val map = MapIO.createMap(Fi(args[1]), true)
                if (args[0] == "add") {
                    if (Main.db!!.maps.getMap(map.file.name()) != null) {
                        result = Result.alreadyAdded
                        return
                    }
                    val dt = Main.db!!.maps.makeNewMapData(map)
                    result = Result.addSuccess
                    setArg(dt.id)
                } else {
                    result = if (Main.db!!.maps.update(map)) {
                        Result.updateSuccess
                    } else {
                        Result.updateFail
                    }
                }
            } catch (ex: IOException) {
                result = Result.invalidFile
                setArg(ex.message!!)
                return
            }
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
        when (args[0]) {
            "enable" -> {
                if (md.isEnabled) {
                    result = Result.alreadyEnabled
                    return
                }
                try {
                    Main.db!!.maps.withdrawMap(mid, MapHandler.mapFolder)
                    if (!Main.testMode) Main.queue!!.post { Vars.maps.reload() } // just in case
                    break
                } catch (e: IOException) {
                    result = Result.enableFail
                    setArg(e.message!!)
                    return
                }
                if (!md.isEnabled) {
                    result = Result.alreadyDisabled
                    return
                }
                try {
                    Main.db!!.maps.hideMap(mid)
                    if (!Main.testMode) Main.queue!!.post { Vars.maps.reload() }
                } catch (e: IOException) {
                    if (args[0] == "disable") log(e)
                }
                if (args[0] == "remove") {
                    Main.db!!.maps.deleteMap(mid)
                }
            }
            "disable" -> {
                if (!md.isEnabled) {
                    result = Result.alreadyDisabled
                    return
                }
                try {
                    Main.db!!.maps.hideMap(mid)
                    if (!Main.testMode) Main.queue!!.post { Vars.maps.reload() }
                } catch (e: IOException) {
                    if (args[0] == "disable") log(e)
                }
                if (args[0] == "remove") {
                    Main.db!!.maps.deleteMap(mid)
                }
            }
            "remove" -> {
                try {
                    Main.db!!.maps.hideMap(mid)
                    if (!Main.testMode) Main.queue!!.post { Vars.maps.reload() }
                } catch (e: IOException) {
                    if (args[0] == "disable") log(e)
                }
                if (args[0] == "remove") {
                    Main.db!!.maps.deleteMap(mid)
                }
            }
        }
    }

    companion object {
        var terminal = MapManager()
        var game: MapManager = object : MapManager() {
            init {
                verifier = Verifier { id: String? -> isPlayerAdmin(main.java.twp.commands.id) }
            }
        }
    }

    init {
        name = "maps"
        argStruct = "<enable/disable/add/remove/update> <filePath/id> [...comment]"
        description = "command for managing maps, only for admins"
    }
}