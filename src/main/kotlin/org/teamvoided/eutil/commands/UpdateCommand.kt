package org.teamvoided.eutil.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

object UpdateCommand {
    fun reg(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val updateNode = CommandManager.literal("update").build()
        dispatcher.root.addChild(updateNode)

        val updatePos1Arg = argument("pos1", BlockPosArgumentType.blockPos())
            .executes { struct(it, BlockPosArgumentType.getBlockPos(it, "pos1"), null) }
            .build()
        updateNode.addChild(updatePos1Arg)

        val updatePos2Arg = argument("pos2", BlockPosArgumentType.blockPos())
            .executes {
                struct(
                    it, BlockPosArgumentType.getBlockPos(it, "pos1"),
                    BlockPosArgumentType.getBlockPos(it, "pos2")
                )
            }
            .build()
        updatePos1Arg.addChild(updatePos2Arg)
    }

    private fun struct(
        c: CommandContext<ServerCommandSource>, pos1: BlockPos, inPos2: BlockPos? = null
    ): Int {
        val src = c.source
        val world: ServerWorld = src.world
        val pos2 = inPos2 ?: src.player?.blockPos ?: return 0

        Thread{
            BlockPos.stream(pos1, pos2).forEach {
                val x = world.getBlockState(it).block
                world.updateNeighborsAlways(it, x)

            }
            src.sendSystemMessage(Text.literal("Done!"))
        }.start()





        return 1
    }
}