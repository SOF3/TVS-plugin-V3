package twp.discord

import twp.tools.Logging.info
import twp.tools.Logging.sendDiscordMessage
import twp.tools.Logging.log
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.listener.message.MessageCreateListener
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.entity.channel.TextChannel
import java.util.NoSuchElementException
import kotlin.Throws
import java.lang.StringBuilder
import org.javacord.api.entity.permission.Role
import org.javacord.api.entity.user.User
import twp.Main
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class Handler(bot: Bot, vararg loader: Loader) : MessageCreateListener {
    var com: ServerTextChannel? = null
    var cur: TextChannel? = null
    var commands = HashMap<String?, Command>()
    fun addCommand(c: Command) {
        commands[c.name] = c
    }

    override fun onMessageCreate(event: MessageCreateEvent) {
        if (event.messageAuthor.isBotUser || !event.messageContent.startsWith(Main.bot!!.cfg!!.prefix)) {
            return
        }
        com = Main.bot!!.Channel(Bot.Channels.commands)
        cur = event.channel
        if (com!!.id != cur.getId()) {
            sendDiscordMessage(cur, "discord-goToCommands", com!!.mentionTag)
            return
        }
        try {
            val ctx = Context(event, this)
            if (!ctx.command!!.can(ctx)) {
                ctx.reply("discord-noPermission", ctx.command!!.listRestrictions())
                return
            }
            ctx.command!!.run(ctx)
        } catch (e: NoSuchCommandException) {
            sendDiscordMessage(cur, "discord-noSuchCommand", Main.bot!!.cfg!!.prefix)
        } catch (e: WrongArgAmountException) {
            sendDiscordMessage(cur, "discord-tooFewArguments", Main.bot!!.cfg!!.prefix)
        } catch (e: NoSuchElementException) {
            sendDiscordMessage(cur, "discord-strangeNoUser")
        } catch (e: Exception) {
            log(e)
        }
    }

    abstract class Command(var args: String) {
        var minArg = 0
        var maxArg = 0
        var minAttachment = 0
        @JvmField
        var name: String? = null
        @JvmField
        var purpose: String? = null
        var restriction = ArrayList<Role>()
        @Throws(NoSuchElementException::class)
        fun can(ctx: Context): Boolean {
            if (ctx.event.isPrivateMessage) return false
            if (restriction.size == 0) return true
            val roles = ctx.event.messageAuthor.asUser().get().getRoles(ctx.event.server.get())
            for (r in roles) {
                for (i in restriction) {
                    if (r.id == i.id) return true
                }
            }
            return false
        }

        fun listRestrictions(): String {
            val sb = StringBuilder()
            for (r in restriction) {
                sb.append("**").append(r.name).append("**")
            }
            return sb.toString()
        }

        abstract fun run(ctx: Context)
        val info: String
            get() = String.format("%s**%s**-*%s*-%s", Main.bot!!.cfg!!.prefix, name, args, purpose)

        init {
            for (i in 0 until args.length) {
                when (args[i]) {
                    '<' -> {
                        minArg++
                        maxArg++
                    }
                    '{' -> minAttachment++
                    '[' -> maxArg++
                }
            }
        }
    }

    class Context internal constructor(var event: MessageCreateEvent, handler: Handler) {
        var name: String
        var content: String
        @JvmField
        var args = arrayOf<String>()
        var user: User
        var channel: TextChannel
        var command: Command?
        fun reply(key: String?, vararg args: Any?) {
            sendDiscordMessage(channel, key, *args)
        }

        init {
            content = event.messageContent
            channel = event.channel
            val all = content.split(" ", 2.toBoolean()).toTypedArray()
            name = all[0].replace(Main.bot!!.cfg!!.prefix, "")
            command = handler.commands[name]
            if (command == null) {
                throw NoSuchCommandException()
            }
            if (all.size == 2) {
                args = all[1].split(" ", command!!.maxArg.coerceAtLeast(0)).toTypedArray()
            }
            if (args.size < command!!.minArg) {
                throw WrongArgAmountException()
            }
            user = event.messageAuthor.asUser().get()
        }
    }

    internal class WrongArgAmountException : Exception()
    internal class NoSuchCommandException : Exception()
    interface Loader {
        fun load(h: Handler)
    }

    init {
        for (l in loader) {
            l.load(this)
        }
        val server = bot.api!!.getServerById(bot.serverID!!)
        if (!server.isPresent) {
            return
        }
        for (c in bot.cfg!!.permissions.keys) {
            val cm = commands[c] ?: continue
            val perms = bot.cfg!!.permissions[c]
            for (p in perms!!) {
                val role = server.get().getRoleById(p)
                if (role.isPresent) {
                    cm.restriction.add(role.get())
                } else {
                    info("discord-roleNotFound", cm.name, p)
                }
            }
        }
    }
}