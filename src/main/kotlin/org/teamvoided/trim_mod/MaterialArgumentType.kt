package org.teamvoided.trim_mod

import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.item.trim.ArmorTrimMaterial
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

object MaterialArgumentType {
    fun materialArg(name: String): RequiredArgumentBuilder<ServerCommandSource, Identifier> {
        return CommandManager.argument(name, IdentifierArgumentType.identifier()).suggests(::listSuggestions)
    }

    @Throws(CommandSyntaxException::class)
    fun getMaterial(context: CommandContext<ServerCommandSource>, name: String): ArmorTrimMaterial {
        try {
            val id = context.getArgument(name, Identifier::class.java)
            println(id)
            val material = context.source.world.registryManager.get(RegistryKeys.TRIM_MATERIAL).get(id)
            if (material == null) {
                throw UNKNOWN_MATERIAL_EXCEPTION.create(id)
            } else {
                return material
            }
        } catch (e: Exception) {
            TrimMod.log.info(e.message)
            e.printStackTrace()
        }
        return null as ArmorTrimMaterial
    }

    private fun listSuggestions(
        commandContext: CommandContext<ServerCommandSource>, suggestionsBuilder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return if (commandContext.source is CommandSource) CommandSource.suggestMatching(
            commandContext.source.world.registryManager.get(RegistryKeys.TRIM_MATERIAL).keys.map { it.value.toString() },
            suggestionsBuilder
        ) else Suggestions.empty()
    }

    private val UNKNOWN_MATERIAL_EXCEPTION =
        DynamicCommandExceptionType { Text.method_54159("Material %s not found!", it) }
}