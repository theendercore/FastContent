package org.teamvoided.eutil

import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.teamvoided.eutil.init.EUtilCommands

@Suppress("unused")
object EUtilMod {
    private const val MODID = "eutil"

    @JvmField
    val log: Logger = LoggerFactory.getLogger(EUtilMod::class.simpleName)

    fun commonInit() {
        log.info("Hello from Common")
        EUtilCommands.init()
    }

    fun clientInit() {
        log.info("Hello from Client")
    }

    fun id(path: String) = Identifier(MODID, path)
}
