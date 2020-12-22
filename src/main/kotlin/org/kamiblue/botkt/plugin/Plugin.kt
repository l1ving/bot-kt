package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.commons.interfaces.Nameable

abstract class Plugin(
    override val name: String
) : Nameable {
    val managers = ArrayList<Manager>()
    val commands = ArrayList<BotCommand>()

    fun register() {
        managers.forEach {
            BotEventBus.subscribe(it)
        }
        commands.forEach {
            CommandManager.register(it)
            BotEventBus.subscribe(it)
        }
    }

    fun unregister() {
        managers.forEach {
            BotEventBus.unsubscribe(it)
        }
        commands.forEach {
            CommandManager.unregister(it)
            BotEventBus.unsubscribe(it)
        }
    }

    abstract fun onLoad()
    abstract fun onUnload()
}