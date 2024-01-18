package org.teamvoided.eutil.init

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.dev.SpawnArmorTrimCommand
import org.teamvoided.eutil.commands.StructureCommand
import org.teamvoided.eutil.commands.TrimCommand
import org.teamvoided.eutil.commands.UpdateCommand

object EUtilCommands {
    fun init() {
        CommandRegistrationCallback.EVENT.register { d, _, _ ->
            SpawnArmorTrimCommand.register(d)
            TrimCommand.reg(d)
            StructureCommand.reg(d)
            UpdateCommand.reg(d)
        }
    }
}