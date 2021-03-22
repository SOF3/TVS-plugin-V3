package twp.discord

import twp.commands.Command
import twp.tools.Logging.translate
import java.lang.StringBuilder
import org.javacord.api.entity.message.embed.EmbedBuilder
import twp.commands.RankSetter
import twp.commands.Searcher
import twp.commands.Informer
import java.awt.Color

class CommandLoader : Handler.Loader {
    override fun load(h: Handler) {
        h.addCommand(object : Handler.Command("none") {
            override fun run(ctx: Handler.Context) {
                val eb = EmbedBuilder()
                    .setTitle("COMMANDS")
                    .setColor(Color.orange)
                val sb = StringBuilder()
                    .append("*!commandName - restriction - <necessary> [optional] {attachment} - description*\n")
                for (c in h.commands.values) {
                    sb.append(c.info).append("\n")
                }
                ctx.channel.sendMessage(eb.setDescription(sb.toString()))
            }

            init {
                name = "help"
                purpose = "shows what you see right now"
            }
        })
        RankSetter.terminal.registerDs(h, null)
        Searcher.discord.registerDs(h, null)
        Informer.general.registerDs(h) { ctx: Handler.Context, self: Command ->
            ctx.channel.sendMessage(
                EmbedBuilder()
                    .setDescription(translate(self.message, *self.arg))
            )
        }
    }
}