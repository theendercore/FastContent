package org.teamvoided.template

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.dev.SpawnArmorTrimCommand
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
object Template {
    private const val MODID = "template"

    @JvmField
    val log: Logger = LoggerFactory.getLogger(Template::class.simpleName)

    fun commonInit() {
        log.info("Hello from Common")
        CommandRegistrationCallback.EVENT.register { d, _, _ ->
            SpawnArmorTrimCommand.register(d)
            TrimCommand.reg(d)
        }
    }

    fun clientInit() {
        log.info("Hello from Client")
    }

    fun id(path: String) = Identifier(MODID, path)
}
