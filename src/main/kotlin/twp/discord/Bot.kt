package twp.discord

import twp.tools.Json.loadJackson
import twp.tools.Logging.info
import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.permission.Role
import twp.Global
import java.lang.Exception
import java.util.HashMap

class Bot(initialized: Boolean) {
    var cfg: Config?
    @JvmField
    var log: Logger
    val api: DiscordApi
    var serverID: Long? = null
    var handler: Handler
    private val channels = HashMap<String?, ServerTextChannel>()
    private val roles = HashMap<String?, Role>()
    fun Channel(ch: Channels): ServerTextChannel? {
        return channels[ch.name]
    }

    enum class Channels {
        liveChat, commandLog, commands, rankLog
    }

    companion object {
        var configDir = Global.config_dir + "/bot"
        var configFile = "$configDir/config.json"
    }

    init {
        cfg = loadJackson(configFile, Config::class.java)
        if (cfg == null) {
            throw RuntimeException("Cannot load config")
        }
        api = try {
            DiscordApiBuilder().setToken(cfg!!.token).login().join()
        } catch (ex: Exception) {
            info("discord-failed")
            throw RuntimeException("Discord failed")
        }
        for (o in cfg!!.channels.keys) {
            val key = o as String
            val optional = api.getServerTextChannelById(cfg!!.channels[key])
            if (!optional.isPresent) {
                info("discord-channelNotFound", key)
                continue
            }
            channels[key] = optional.get()
            if (serverID == null) serverID = optional.get().server.id
        }
        for (o in cfg!!.roles.keys) {
            val key = o as String
            val optional = api.getRoleById(cfg!!.roles[key])
            if (!optional.isPresent) {
                println("$key role not found.")
                continue
            }
            roles[key] = optional.get()
            if (serverID == null) serverID = optional.get().server.id
        }
        log = Logger(initialized)
        api.addMessageCreateListener(log)
        handler = Handler(this, CommandLoader())
        api.addMessageCreateListener(handler)
    }
}