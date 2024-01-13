package org.teamvoided.trim_mod

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.dev.SpawnArmorTrimCommand
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
object TrimMod {
    private const val MODID = "trim_mod"

    @JvmField
    val log: Logger = LoggerFactory.getLogger(TrimMod::class.simpleName)

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
