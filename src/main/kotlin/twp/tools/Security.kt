package twp.tools

import java.lang.Math
import kotlin.jvm.JvmStatic
import org.apache.commons.codec.digest.DigestUtils
import twp.Main

// some hash functions
object Security {
    // ignore this big mistake pls
    @JvmStatic
    fun hash(password: String): Long {
        var password = password
        password += Main.config.salt
        var res: Long = 0
        for (i in 0 until password.length) {
            res = res + Math.pow(password[i].toDouble(), 2.0).toLong()
        }
        return res
    }

    @JvmStatic
    fun hash2(password: String): String {
        return DigestUtils.sha256Hex(password + Main.config.salt)
    }
}