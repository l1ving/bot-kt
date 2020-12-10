package org.kamiblue.botkt

import kotlinx.coroutines.delay
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.helpers.UpdateHelper
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.util.*


/**
 * @author l1ving
 * @since 16/08/20 17:30
 */
object Bot {

    suspend fun start() {
        val started = System.currentTimeMillis()

        MessageSendUtils.log("Starting bot!")

        UpdateHelper.writeVersion(Main.currentVersion)
        UpdateHelper.updateCheck()

        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            MessageSendUtils.log("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            Main.exit()
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
            // Annotation based Event Listener
            addListener(this@Bot)
        }

        CommandManager.init()

        val initMessage = "Initialized bot!\n" +
            "Running on ${Main.currentVersion}\n" +
            "Startup took ${System.currentTimeMillis() - started}ms"

        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

        updateStatus(userConfig)

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed

        sendStartupMessage(userConfig, initMessage)

        Main.ready = true
        MessageSendUtils.log(initMessage)
    }

    private fun updateStatus(userConfig: UserConfig?) {
        userConfig?.statusMessage?.let { message ->
            val type = userConfig.statusMessageType?.let {
                ActivityType.values().getOrNull(it)
            } ?: ActivityType.UNKNOWN

            Main.client.updateStatus(UserStatus.ONLINE, type, message)
        }
    }

    private suspend fun sendStartupMessage(userConfig: UserConfig?, initMessage: String) {
        userConfig?.startUpChannel?.let {
            if (userConfig.primaryServerId == null) {
                Main.client.servers.forEach { server ->
                    delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                    server.textChannels.findByName(it)?.send {
                        embed {
                            title = "Startup"
                            description = initMessage
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            } else {
                val channel = Main.client.servers.find(userConfig.primaryServerId)?.textChannels?.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initMessage
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!Main.ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val string = if (event.message.content[0] == Main.prefix) event.message.content.substring(1) else return

        CommandManager.submit(event, string)
    }

}