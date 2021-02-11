package org.kamiblue.botkt.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.kamiblue.commons.collections.NameableSet
import org.kamiblue.commons.interfaces.Nameable
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap

abstract class AbstractConfig(
    final override val name: String,
) : Nameable {
    protected abstract val path: String

    private val settings = NameableSet<Setting<*>>(LinkedHashMap())
    private val fileName = name.toLowerCase(Locale.ROOT).replace(' ', '_')

    private val directory: File
        get() = File(path)

    private val file: File
        get() = File("$path/$fileName.json")

    protected fun <T : Any> setting(name: String, value: T, description: String = "No description", consumer: (T, T) -> T = { _, it -> it }): Setting<T> {
        return Setting(name, value, description, consumer).also {
            settings.add(it)
        }
    }

    fun reset() {
        settings.forEach(Setting<*>::reset)
    }

    fun load() {
        val file = file
        if (!file.exists()) return

        val jsonObject = file.bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
        }

        for (setting in settings) {
            val jsonElement = jsonObject[setting.jsonName] ?: continue
            setting.read(jsonElement)
        }
    }

    fun save() {
        directory.run {
            if (!exists()) {
                mkdirs()
            }
        }

        val file = file
        if (!file.exists()) {
            file.createNewFile()
        }

        val jsonObject = JsonObject()
        for (setting in settings) {
            jsonObject.add(setting.jsonName, setting.write())
        }

        file.bufferedWriter().use {
            gson.toJson(jsonObject, it)
        }
    }

    override fun toString(): String {
        return settings.joinToString(prefix = "{", postfix = "}")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractConfig) return false

        if (name != other.name) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    protected companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        fun <T> Iterable<T>.runCatchingAll(block: (T) -> Unit, catch: (T, Exception) -> Unit): Boolean {
            var success = true
            this.forEach {
                try {
                    block(it)
                } catch (e: Exception) {
                    success = false
                    catch(it, e)
                }
            }
            return success
        }
    }

}

