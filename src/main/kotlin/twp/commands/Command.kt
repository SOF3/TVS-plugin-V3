package twp.commands

import arc.func.Cons
import arc.util.CommandHandler
import arc.util.Strings
import twp.tools.Logging.log
import twp.tools.Logging.info
import twp.database.PD
import mindustry.gen.Player
import org.junit.platform.commons.util.ExceptionUtils
import twp.Main
import twp.discord.Handler
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.jvm.JvmOverloads
import java.lang.RuntimeException
import java.util.*
import java.util.function.Consumer

// Command is base class of any command and contains utility for making commands bit more cleaner and organised
// One good advice, dont write your game in java... newer.
abstract class Command {
    var freeAccess = false
    var verifier: Verifier = { _: String? -> true }

    // constant
    var name = "noname"
    var argStruct: String? = null
    var description = "description missing"

    // dynamic
    var result = Result.success
    var argField: Array<out Any> = arrayOf()
    var caller: PD? = null

    // main behavior
    abstract fun run(id: String?, vararg args: String?)
    fun setArg(vararg values: Any) {
        argField = values
    }

    // Shorthand for checking whether correct amount of arguments were provided
    fun checkArgCount(count: Int, supposed: Int): Boolean {
        if (count < supposed) {
            setArg(count, supposed)
            result = Result.notEnoughArgs
            return true
        }
        return false
    }

    // Shorthand for checking and handling invalid non integer arguments
    fun isNotInteger(args: Array<String?>, idx: Int): Boolean {
        if (Strings.canParsePositiveInt(args[idx])) {
            return false
        }
        setArg(idx + 1, args[idx]!!)
        result = Result.notInteger
        return true
    }

    // for registration of commandline commands
    fun registerTm(handler: CommandHandler, runner: TerminalCommandRunner?) {
        freeAccess = true
        val func = Cons { args: Array<String?> ->
            try {
                result = Result.success
                run("", *args)
                if (runner != null) {
                    runner(this)
                } else {
                    notifyCaller()
                }
            } catch (ex: Exception) {
                result = Result.bug
                try {
                    notifyCaller()
                } catch (e: Exception) {
                    log(ex)
                }
                log(ex)
            }
        }
        if (argStruct == null) {
            handler.register(name, description, func)
        } else {
            handler.register(name, argStruct, description, func)
        }
    }

    // For registration of in-game commands
    fun registerGm(handler: CommandHandler, runner: PlayerCommandRunner?) {
        val run = CommandHandler.CommandRunner { args: Array<String?>, player: Player ->
            val pd = Main.db.online[player.uuid()]
            if (pd == null) {
                log("null PD when executing $name command.")
                player.sendMessage(
                    "[yellow]Sorry there is an problem with your profile, " +
                            "try reconnecting or contact admins that you see this message."
                )
                return@CommandRunner
            }
            caller = pd
            try {
                result = Result.success
                run(player.uuid(), *args)
                if (runner != null) {
                    runner(this, pd)
                } else {
                    notifyCaller()
                }
            } catch (ex: Exception) {
                result = Result.bug
                // fucking java and deadlocks
                try {
                    notifyCaller()
                } catch (e: Exception) {
                    log(e)
                }
                log(ex)
            }
        }
        if (argStruct == null) {
            handler.register(name, description, run)
        } else {
            handler.register(name, argStruct, description, run)
        }
    }

    fun registerDs(handler: Handler, run: DiscordCommandRunner?) {
        handler.addCommand(object : Handler.Command(argStruct!!) {
            override fun run(ctx: Handler.Context) {
                Main.queue.post {
                    try {
                        result = Result.success
                        this@Command.run("", *ctx.args)
                        if (run != null) {
                            run(ctx, this@Command)
                        } else {
                            ctx.reply(message, *argField)
                        }
                    } catch (e: Exception) {
                        ctx.reply("discord-internalError", ExceptionUtils.readStackTrace(e))
                    }
                }
            }

            init {
                name = this@Command.name
                purpose = description
            }
        })
    }

    // getMessage returns bundle key based of result
    val message: String
        get() = getMessage(result)

    // getMessage returns bundle key based of result
    fun getMessage(r: Result): String {
        return (if (r.general) "" else "$name-") + r.name
    }

    // notifyCaller sends message to caller, its just a shorthand and is atomaticly called if
    // command lambda is null
    fun notifyCaller() {
        if (result == Result.none) {
            return
        }
        if (caller == null) {
            info(message, *argField)
            return
        }
        caller!!.sendServerMessage(message, *argField)
    }

    // shorthand for kicking caller
    fun kickCaller(duration: Int) {
        caller!!.kick(message, duration, *argField)
    }

    // creates string of information about online players
    fun listPlayers(): String {
        val sb = StringBuilder()
        for (pd in Main.db.online.values) {
            sb.append(pd.summarize()).append("\n")
        }
        return sb.substring(0, sb.length - 1)
    }

    fun isPlayerAdmin(id: String?): Boolean {
        val pd = Main.db.online[id]
        return pd != null && pd.rank.admin
    }

    fun playerNotFound() {
        result = Result.playerNotFound
        setArg(listPlayers())
    }

    fun cannotInteract(id: String?): Boolean {
        if (Main.db.online[id]!!.cannotInteract()) {
            result = Result.noPerm
            return true
        }
        return false
    }

    fun wrongOption(idx: Int, args: Array<String>, options: String): Boolean {
        return wrongOption(idx, args, Arrays.asList(*options.split(" ").toTypedArray()), null)
    }

    @JvmOverloads
    fun wrongOption(idx: Int, args: Array<String>, options: List<String>?, raw: String? = null): Boolean {
        var raw = raw
        if (options == null) {
            result = Result.wrongOption
            setArg(idx, raw!!)
            return false
        } else if (!options.contains(args[idx])) {
            result = Result.wrongOption
            if (raw == null) {
                val sb = StringBuilder()
                options.forEach(Consumer { e: String? -> sb.append(e).append(" ") })
                raw = sb.toString()
            }
            setArg(idx, raw)
            return true
        }
        return false
    }

    // Used for testing commands
    fun assertResult(supposed: Result) {
        try {
            if (result != Result.none) {
                info(message, *argField)
            }
            if (supposed != result) {
                throw RuntimeException(supposed.name + "!=" + result.name)
            }
        } finally {
            result = Result.success
        }
    }


    // Result enum contains all possible results command can return
    enum class Result {
        success, notFound, notExplicit, noPerm, wrongRank, wrongAccess, unprotectSuccess, alreadyProtected, confirm, confirmFail, confirmSuccess, invalidRequest, loginSuccess, unsetSuccess, emptySlice, invalidSlice, noOneOnline, successOnline, wrongCommand, redundant, cannotApplyToSelf, notExist, invalidFile, enableFail, addSuccess, alreadyEnabled, alreadyDisabled, disableFail, updateSuccess, updateFail, alreadyAdded, recoverSuccess, penalty, start, hint, testFail, bug(
            true
        ),
        notInteger(true), playerNotFound(true), notEnoughArgs(true), incorrectPassword(true), alreadyVoting(true), cannotVote(
            true
        ),
        voteStartSuccess(true), invalidVoteSession(true), voteSuccess(true), alreadyVoted(true), wrongOption(true), none, rateSuccess, info, stats, fail, invalidSearch;

        var general = false

        constructor() {}
        constructor(general: Boolean) {
            this.general = general
        }
    }
}

// lambda for commands invoiced by players
typealias PlayerCommandRunner = (Command, PD) -> Unit

// lambda for commands called from command prompt
typealias TerminalCommandRunner = (c: Command?) -> Unit

typealias DiscordCommandRunner = (ctx: Handler.Context?, self: Command?) -> Unit

typealias Verifier = (id: String?) -> Boolean