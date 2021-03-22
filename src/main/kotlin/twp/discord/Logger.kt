package twp.discord

import twp.tools.Logging.on
import twp.tools.Logging.translate
import org.javacord.api.listener.message.MessageCreateListener
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.entity.channel.TextChannel
import twp.database.enums.RankType
import twp.database.Rank
import mindustry.game.EventType
import org.javacord.api.entity.channel.Channel
import twp.Main

class Logger(initialized: Boolean) : MessageCreateListener {
    override fun onMessageCreate(event: MessageCreateEvent) {
        val at = event.messageAuthor
        if (at.isBotUser) {
            return
        }
        val curr: Channel = event.channel
        val chn: TextChannel? = Main.bot!!.Channel(Bot.Channels.liveChat)
        if (chn != null && chn.id == curr.id) {
            for (pd in Main.db!!.online.values) {
                pd.sendDiscordMessage(event.messageContent, at.name)
            }
        }
    }

    fun logRankChange(id: Long, rank: Rank, comment: String?) {
        val chn: TextChannel? = Main.bot!!.Channel(Bot.Channels.rankLog)
        val ac = Main.db!!.handler.getAccount(id)
        if (chn != null && ac != null) {
            chn.sendMessage(
                String.format(
                    "player:**%s**(id:%d)\nchange: %s -> %s\nreason: %s",
                    ac.name,
                    id,
                    ac.getRank(RankType.rank).suffix,
                    rank.suffix,
                    comment
                )
            )
        }
    }

    init {
        if (initialized) return
        on(EventType.PlayerChatEvent::class.java) { e: EventType.PlayerChatEvent ->
            if (Main.bot == null) return@on
            val chn = Main.bot!!.Channel(Bot.Channels.commandLog) ?: return@on
            if (e.message.startsWith("/") && !e.message.startsWith("/account")) {
                val pd = Main.db!!.online[e.player.uuid()]
                chn.sendMessage(translate("discord-commandLog", pd!!.player.name, pd.id, pd.rank.name, e.message))
            }
        }
        on(EventType.PlayerChatEvent::class.java) { e: EventType.PlayerChatEvent ->
            if (Main.bot == null) return@on
            val chn = Main.bot!!.Channel(Bot.Channels.liveChat) ?: return@on
            if (!e.message.startsWith("/")) {
                val pd = Main.db!!.online[e.player.uuid()]
                chn.sendMessage(translate("discord-serverMessage", pd!!.player.name, pd.id, e.message))
            }
        }
    }
}