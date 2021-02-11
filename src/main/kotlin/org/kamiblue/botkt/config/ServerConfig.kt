package org.kamiblue.botkt.config

import org.kamiblue.botkt.Main

open class ServerConfig(name: String) : AbstractConfig(name) {

    final override val path: String
        get() = "configs/$server"

    var server = -1L; private set

    fun init(server: Long) {
        this.server = server
    }

    companion object {
        private val serverConfigs = LinkedHashMap<Class<out ServerConfig>, LinkedHashMap<Long, ServerConfig>>()

        inline fun <reified T : ServerConfig> register() {
            register(T::class.java)
        }

        fun <T : ServerConfig> register(clazz: Class<out T>) {
            serverConfigs.computeIfAbsent(clazz) { LinkedHashMap() }
        }

        inline fun <reified T : ServerConfig> unregister() {
            unregister(T::class.java)
        }

        fun <T : ServerConfig> unregister(clazz: Class<out T>) {
            serverConfigs.remove(clazz)
        }

        fun loadAll(): Boolean {
            return serverConfigs.entries.runCatchingAll(
                { (config, map) ->
                    map.values.runCatchingAll(ServerConfig::load) { it, e ->
                        Main.logger.warn("Failed to load server config ${config.name} for server $it", e)
                    }
                },
                { it, e ->
                    Main.logger.warn("Failed to load server config ${it.key.name}", e)
                }
            )
        }

        fun saveAll(): Boolean {
            return serverConfigs.entries.runCatchingAll(
                { (config, map) ->
                    map.values.runCatchingAll(ServerConfig::save) { it, e ->
                        Main.logger.warn("Failed to save server config ${config.name} for server $it", e)
                    }
                },
                { it, e ->
                    Main.logger.warn("Failed to save server config ${it.key.name}", e)
                }
            )
        }

        inline fun <reified T : ServerConfig> load(): Boolean {
            return load(T::class.java)
        }

        fun <T : ServerConfig> load(clazz: Class<out T>): Boolean {
            return try {
                getServerConfigMap(clazz).values.runCatchingAll(ServerConfig::load) { it, e ->
                    Main.logger.warn("Failed to load server config ${it.name} for server ${it.server}", e)
                }
                true
            } catch (e: Exception) {
                Main.logger.warn("Failed to load server config ${clazz.simpleName}", e)
                false
            }
        }

        inline fun <reified T : ServerConfig> save(): Boolean {
            return save(T::class.java)
        }

        fun <T : ServerConfig> save(clazz: Class<out T>): Boolean {
            return try {
                getServerConfigMap(clazz).values.runCatchingAll(ServerConfig::load) { it, e ->
                    Main.logger.warn("Failed to save server config ${it.name} for server ${it.server}", e)
                }
                true
            } catch (e: Exception) {
                Main.logger.warn("Failed to save server config ${clazz.simpleName}", e)
                false
            }
        }

        inline operator fun <reified T : ServerConfig> get(server: Long): T {
            return get(server, T::class.java)
        }

        fun <T : ServerConfig> get(server: Long, clazz: Class<out T>): T {
            return getServerConfigMap(clazz).getServerInstance(server, clazz)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : ServerConfig> getServerConfigMap(clazz: Class<out T>): MutableMap<Long, T> {
            val map = serverConfigs[clazz]
                ?: throw IllegalArgumentException("Server config type ${clazz.simpleName} is not registered")

            return map as LinkedHashMap<Long, T>
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : ServerConfig> MutableMap<Long, T>.getServerInstance(
            server: Long,
            clazz: Class<out T>
        ): T {
            val config = this[server]

            return if (config == null) {
                val newConfig = clazz.constructors.first().newInstance() as T
                newConfig.init(server)
                newConfig.load()

                this[server] = newConfig
                newConfig
            } else {
                config
            }
        }
    }
}
