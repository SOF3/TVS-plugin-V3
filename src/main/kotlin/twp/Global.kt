package twp

import twp.tools.Json
import java.util.HashMap

// plugin config is stored here
object Global {
    const val dir = "config/mods/worse"
    const val config_dir = dir + "/config"
    const val save_dir = config_dir + "/saves"
    const val config_file = config_dir + "/config.json"
    const val weapons = config_dir + "/weapons.json"
    const val weaponLinking = config_dir + "/weapon_linking.json"
    fun loadConfig(): Config {
        return Json.loadJackson(
            config_file,
            Config::class.java
        )
            ?: return Config()
    }

    class Config {
        @JvmField
        var salt = "TWS"
        var rules: HashMap<String, String>? = null
        var guide: HashMap<String, String>? = null
        var welcomeMessage: HashMap<String, String>? = null
        @JvmField
        var consideredPassive = 10 // after how many missed votes is player considered passive
        @JvmField
        var shipLimit = 3 // maximum amount of ships players acn have
        @JvmField
        var doubleClickSpacing: Long = 300 // double click sensitivity
        @JvmField
        var maxNameLength = 25 // if name is longer then this amount it is truncated
        @JvmField
        var testPenalty = (15 * 60 * 1000 // how often can players take test
                ).toLong()
        @JvmField
        var sec = Security()
        var vpn = VPN()
        @JvmField
        var db = Database()
        @JvmField
        var loadout = Loadout()
    }

    class Database {
        @JvmField
        var name = "mindustryServer" // database name
        @JvmField
        var players = "PlayerData" // player collection name
        @JvmField
        var maps = "MapData" // map collection name
        @JvmField
        var address = "mongodb://127.0.0.1:27017" // database host
    }

    class Loadout {
        @JvmField
        var name = "Loadout" // loadout collection
        @JvmField
        var shipTravelTime = 60 * 3
        @JvmField
        var shipCapacity = 3000
    }

    class Security {
        @JvmField
        var actionLimit = 50 // how many actions triggers protection
        @JvmField
        var actionLimitFrame = (1000 * 2 // how frequently is action counter reset
                ).toLong()
        @JvmField
        var actionUndoTime = (1000 * 10 // how old actions will be reverted after protection trigger
                ).toLong()
        @JvmField
        var actionMemorySize = 5 // how many actions will be saved in tile for inspection
    }

    class VPN {
        var api: String? = null
        var timeout = 0
    }
}