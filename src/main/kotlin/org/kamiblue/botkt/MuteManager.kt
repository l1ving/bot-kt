package org.kamiblue.botkt

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import net.ayataka.kordis.entity.botUser
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.role.Role
import net.ayataka.kordis.utils.timer
import java.io.*
import java.util.concurrent.ConcurrentHashMap

object MuteManager {

    val serverMap = HashMap<Long, ServerMuteInfo>() // <Server ID, ServerMuteInfo>
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object: TypeToken<LinkedHashMap<Long, Map<Long, Long>>>(){}.type
    private val muteFile = File("config/mute.json")

    fun save() {
        BufferedWriter(FileWriter(muteFile)).use {
            val cacheMap = LinkedHashMap<Long, Map<Long, Long>>()

            for ((id, serverMuteInfo) in serverMap) {
                cacheMap[id] = serverMuteInfo.muteMap
            }

            gson.toJson(cacheMap, it)
        }
    }

    fun load() {
        BufferedReader(FileReader(muteFile)).use { reader ->
            val cacheMap = gson.fromJson<LinkedHashMap<Long, Map<Long, Long>>>(reader, type)
            for ((id, cacheMuteMap) in cacheMap) {
                serverMap.getOrPut(id) {
                    ServerMuteInfo(Main.client.servers.find(id)!!)
                }.apply {
                    muteMap.clear()
                    muteMap.putAll(cacheMuteMap)
                }
            }
        }
    }

    class ServerMuteInfo(val server: Server) {
        val muteMap = ConcurrentHashMap<Long, Long>() // <Member ID, Unmute Time>
        private val coroutineMap = HashMap<Long, Job>() // <Member ID, Coroutine Job>

        private var mutedRole: Role? = null

        suspend fun getMutedRole() = mutedRole
            ?: server.roles.findByName("Muted")
            ?: server.createRole {
                name = "Muted"
                permissions = PermissionSet(server.roles.everyone.permissions.compile() and 68224001)
                position = server.members.botUser.roles.map { it.position }.maxOrNull()!!
            }

        suspend fun startUnmuteCoroutine(
            member: Member,
            role: Role,
            duration: Long
        ) {
            coroutineMap[member.id] = GlobalScope.launch {
                delay(duration)
                member.removeRole(role)
                muteMap.remove(member.id)
                coroutineMap.remove(member.id)
            }
        }

        init {
            GlobalScope.launch {
                delay(5000L)
                while (isActive) {
                    for ((id, unmuteTime) in muteMap) {
                        delay(500L)
                        if (!coroutineMap.contains(id)) {
                            val member = server.members.find(id) ?: continue
                            val duration = unmuteTime - System.currentTimeMillis()
                            startUnmuteCoroutine(member, getMutedRole(), duration)
                        }
                    }
                }
            }
        }
    }

    init {
        GlobalScope.timer(30000L) {
            try {
                delay(30000L)
                save()
            } catch (e: Exception) {

            }
        }
    }

}