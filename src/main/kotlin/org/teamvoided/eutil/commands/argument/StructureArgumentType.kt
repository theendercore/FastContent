package org.teamvoided.eutil.commands.argument

object StructureArgumentType {
//    fun patternArg(name: String): RequiredArgumentBuilder<ServerCommandSource, Identifier> {
//        return CommandManager.argument(name, IdentifierArgumentType.identifier())
//            .suggests(PatterArgumentType::listSuggestions)
//    }
//
//    @Throws(CommandSyntaxException::class)
//    fun getPattern(context: CommandContext<ServerCommandSource>, name: String): ArmorTrimPattern {
//        try {
//            val id = context.getArgument(name, Identifier::class.java)
//            println(id)
//            val pattern = context.source.world.registryManager.get(RegistryKeys.TRIM_PATTERN).get(id)
//            if (pattern == null) {
//                throw UNKNOWN_PATTERN_EXCEPTION.create(id)
//            } else {
//                return pattern
//            }
//        } catch (e: Exception) {
//            TrimMod.log.info(e.message)
//            e.printStackTrace()
//        }
//        return null as ArmorTrimPattern
//    }
//
//    private fun listSuggestions(
//        commandContext: CommandContext<ServerCommandSource>, suggestionsBuilder: SuggestionsBuilder
//    ): CompletableFuture<Suggestions> {
//        return if (commandContext.source is CommandSource) CommandSource.suggestMatching(
//            commandContext.source.world.registryManager.get(RegistryKeys.TRIM_PATTERN).keys.map { it.value.toString() },
//            suggestionsBuilder
//        ) else Suggestions.empty()
//    }
//
//    private val UNKNOWN_PATTERN_EXCEPTION =
//        DynamicCommandExceptionType { Text.method_54159("Pattern %s not found!", it) }
}