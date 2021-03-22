package twp.tools

import arc.Events
import arc.func.Cons
import arc.util.Log
import java.io.File
import java.io.IOException
import kotlin.jvm.JvmStatic
import java.text.SimpleDateFormat
import org.javacord.api.entity.channel.TextChannel
import java.util.concurrent.CompletableFuture
import java.lang.RuntimeException
import java.lang.Runnable
import org.junit.platform.commons.util.ExceptionUtils
import java.io.PrintWriter
import java.io.FileOutputStream
import org.javacord.api.entity.message.Message
import twp.Global
import twp.Main
import java.lang.Exception
import java.sql.Date

// Serializing errors and sending messages to cmd is performed from here
object Logging {
    const val outDir = Global.dir + "/errors/"
    var formatterDate = SimpleDateFormat("yyyy-MM-dd z")
    var formatterTime = SimpleDateFormat("[HH-mm-ss-SSS]")
    @JvmStatic
    fun main(args: Array<String>) {
        log("hello")
        log("hello")
    }

    @JvmStatic
    fun sendDiscordMessage(ch: TextChannel, key: String?, vararg args: Any?): CompletableFuture<Message> {
        return ch.sendMessage(translate(key, *args))
    }

    @JvmStatic
    fun translate(key: String?, vararg args: Any?): String? {
        return Text.cleanColors(Text.format(Main.bundle.getDefault(key), *args))
    }

    @JvmStatic
    fun info(key: String?, vararg args: Any?) {
        Log.info(translate(key, *args))
    }

    fun log(message: String?) {
        log(RuntimeException(message))
    }

    fun sendMessage(key: String?, vararg args: Any?) {
        for (pd in Main.db!!.online.values) {
            pd.sendServerMessage(key, *args)
        }
    }

    fun log(run: Runnable) {
        try {
            run.run()
        } catch (e: Exception) {
            log(e)
        }
    }

    @JvmStatic
    fun log(t: Throwable) {
        val ex = ExceptionUtils.readStackTrace(t)
        val date = Date(System.currentTimeMillis())
        val f = File(outDir + formatterDate.format(date))
        t.printStackTrace()
        try {
            Json.makeFullPath(f.absolutePath)
            if (!f.exists()) {
                f.createNewFile()
            }
            val out = PrintWriter(FileOutputStream(f, true))
            out.println(formatterTime.format(date))
            out.println(ex)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun <T> on(event: Class<T>?, cons: Cons<T>) {
        Events.on(event) { e: T ->
            try {
                cons[e]
            } catch (ex: Exception) {
                log(ex)
            }
        }
    }

    fun run(event: Any?, listener: Runnable) {
        Events.run(event) {
            try {
                listener.run()
            } catch (ex: Exception) {
                log(ex)
            }
        }
    }
}