package twp.discord

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.HashMap

class Config {
    var prefix = "!"
    var token = "put it there neeeeebaaaaa"
    var roles = HashMap<String?, String?>()
    var channels: HashMap<String?, String?> = object : HashMap<String?, String?>() {
        init {
            put(Bot.Channels.liveChat.name, "")
            put(Bot.Channels.commandLog.name, "")
            put(Bot.Channels.commands.name, "")
        }
    }
    var permissions: HashMap<String?, Array<String>> = object : HashMap<String?, Array<String?>?>() {
        init {
            put("setrank", arrayOf<String>("admin role here", "other roles..."))
        }
    }

    constructor() {}

    @JsonCreator
    constructor(
        @JsonProperty("prefix") prefix: String?,
        @JsonProperty("token") token: String?,
        @JsonProperty("roles") roles: HashMap<String?, String?>?,
        @JsonProperty("channels") channels: HashMap<String?, String?>?,
        @JsonProperty("permissions") permissions: HashMap<String?, Array<String>>
    ) {
        if (prefix != null) {
            this.prefix = prefix
        }
        if (token != null) {
            this.token = token
        }
        if (roles != null) {
            this.roles = roles
        }
        if (channels != null) {
            this.channels = channels
        }
        if (roles != null) {
            this.permissions = permissions
        }
    }
}