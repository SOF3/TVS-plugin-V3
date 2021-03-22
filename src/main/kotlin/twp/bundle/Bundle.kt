package twp.bundle

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import twp.database.PD
import org.jsoup.Jsoup
import twp.tools.Logging
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

// Some bundle abstraction
class Bundle {
    val locale = Locale("en", "US")
    val defaultBundle = newBundle(locale)
    fun newBundle(locale: Locale?): ResourceBundle {
        return ResourceBundle.getBundle(bundlePath, locale, UTF8Control())
    }

    fun getLocData(ip: String): HashMap<String, Any>? {
        val mapper = ObjectMapper()
        val jt: JavaType =
            mapper.typeFactory.constructMapLikeType(HashMap::class.java, String::class.java, Any::class.java)
        try {
            val json = Jsoup.connect("http://ipapi.co/$ip/json").ignoreContentType(true).timeout(3000).execute().body()
            return mapper.readValue(json, jt)
        } catch (e: IOException) {
            Logging.log(e)
        }
        return null
    }

    fun getLocale(ip: String): Locale {
        val data = getLocData(ip) ?: return locale
        val languages = data["languages"] as String? ?: return locale
        val resolvedL = languages.split(",").toTypedArray()
        if (resolvedL.size == 0) return locale
        val resResolvedL = resolvedL[0].split("-").toTypedArray()
        return if (resResolvedL.size < 2) locale else Locale(resResolvedL[0], resResolvedL[1])
    }

    fun resolveBundle(pd: PD) {
        Thread { pd.setBundle(newBundle(getLocale(pd.player.ip))) }
    }

    fun getDefault(key: String?): String {
        return if (defaultBundle.containsKey(key)) {
            defaultBundle.getString(key)
        } else String.format("bundle key %s is missing, please report it", key)
    }

    class UTF8Control : ResourceBundle.Control() {
        @Throws(IllegalAccessException::class, InstantiationException::class, IOException::class)
        override fun newBundle(
            baseName: String,
            locale: Locale,
            format: String,
            loader: ClassLoader,
            reload: Boolean
        ): ResourceBundle {
            // The below is a copy of the default implementation.
            val bundleName = toBundleName(baseName, locale)
            val resourceName = toResourceName(bundleName, "properties")
            var bundle: ResourceBundle? = null
            var stream: InputStream? = null
            if (reload) {
                val url = loader.getResource(resourceName)
                if (url != null) {
                    val connection = url.openConnection()
                    if (connection != null) {
                        connection.useCaches = false
                        stream = connection.getInputStream()
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName)
            }
            if (stream != null) {
                bundle = try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    PropertyResourceBundle(InputStreamReader(stream, StandardCharsets.UTF_8))
                } finally {
                    stream.close()
                }
            }
            return bundle!!
        }
    }

    companion object {
        const val bundlePath = "tws-bundles.bundle"
    }
}