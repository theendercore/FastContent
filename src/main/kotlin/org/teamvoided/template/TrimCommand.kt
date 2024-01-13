package org.teamvoided.template

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object TrimCommand {

    fun reg(dispatcher: CommandDispatcher<ServerCommandSource>){
        val trimNode = CommandManager.literal("trim").executes(::trim).build()
        dispatcher.root.addChild(trimNode)
    }

    fun trim(c: CommandContext<ServerCommandSource>): Int{
        return 1
    }
}