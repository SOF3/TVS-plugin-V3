package twp.tools

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import kotlin.Throws
import kotlin.jvm.JvmStatic
import java.lang.Exception
import java.util.HashMap

// Json loading ans saving logic
object Json {
    // loadHashMap loads a hashmap with given key and also saves optional hashmap parameter
    @JvmStatic
    fun <V> loadHashmap(filename: String?, `val`: Class<V>?, def: HashMap<String, V>?): HashMap<String, V>? {
        val mapper = ObjectMapper()
        val jt: JavaType = mapper.typeFactory.constructMapLikeType(HashMap::class.java, String::class.java, `val`)
        val fi = File(filename)
        if (!fi.exists()) {
            if (def == null) {
                return null
            }
            saveSimple(filename, def)
            return def
        }
        return try {
            mapper.readValue(File(filename), jt)
        } catch (e: IOException) {
            Logging.info("json-failLoad", filename, e.message)
            Logging.log(e)
            null
        }
    }

    fun saveSimple(filename: String?, obj: Any?) {
        val mapper = ObjectMapper()
        try {
            makeFullPath(filename)
            mapper.writeValue(File(filename), obj)
        } catch (e: IOException) {
            Logging.info("json-failSave", filename, e.message)
        }
    }

    @Throws(IOException::class)
    fun makeFullPath(filename: String?) {
        val targetFile = File(filename)
        val parent = targetFile.parentFile
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Couldn't create dir: $parent")
        }
    }

    @JvmStatic
    fun <T> loadJackson(filename: String?, type: Class<T>): T? {
        val mapper = ObjectMapper()
        val f = File(filename)
        return try {
            if (!f.exists()) {
                saveJackson(filename, type)
            } else mapper.readValue(f, type)
        } catch (e: IOException) {
            Logging.info("json-failLoad", filename, e.message)
            null
        }
    }

    fun <T> saveJackson(filename: String?, type: Class<T>): T? {
        val mapper = ObjectMapper()
        try {
            makeFullPath(filename)
            val f = File(filename)
            val obj = type.getDeclaredConstructor().newInstance()
            mapper.writeValue(f, obj)
            return obj
        } catch (e: Exception) {
            Logging.info("json-failSave", filename, e.message)
        }
        return null
    }
}