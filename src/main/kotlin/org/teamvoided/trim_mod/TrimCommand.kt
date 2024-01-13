package org.teamvoided.trim_mod

import com.google.common.collect.Maps
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.datafixers.util.Pair
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.*
import net.minecraft.item.trim.ArmorTrimMaterial
import net.minecraft.item.trim.ArmorTrimPattern
import net.minecraft.item.trim.ArmorTrimPermutation
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import org.teamvoided.trim_mod.MaterialArgumentType.materialArg
import org.teamvoided.trim_mod.PatterArgumentType.patternArg

object TrimCommand {

    fun reg(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val trimNode = literal("trim").build()
        dispatcher.root.addChild(trimNode)

        val trimNodeBlockPodArg = argument("pos", BlockPosArgumentType.blockPos()).build()
        trimNode.addChild(trimNodeBlockPodArg)

        val allNode = literal("all").executes { all(it, BlockPosArgumentType.getBlockPos(it, "pos")) }.build()
        trimNodeBlockPodArg.addChild(allNode)
        val allNodeItemsArg =
            literal("items").executes { all(it, BlockPosArgumentType.getBlockPos(it, "pos"), true) }.build()
        allNode.addChild(allNodeItemsArg)

        val patNode = literal("pattern").build()
        trimNodeBlockPodArg.addChild(patNode)
        val patNodePatArg = patternArg("pattern").executes {
            pat(
                it,
                PatterArgumentType.getPattern(it, "pattern"),
                BlockPosArgumentType.getBlockPos(it, "pos")
            )
        }.build()
        patNode.addChild(patNodePatArg)
        val patNodePatArgItemsArg = literal("items").executes {
            pat(
                it,
                PatterArgumentType.getPattern(it, "pattern"),
                BlockPosArgumentType.getBlockPos(it, "pos"),
                true
            )
        }.build()
        patNodePatArg.addChild(patNodePatArgItemsArg)

        val matNode = literal("material").build()
        trimNodeBlockPodArg.addChild(matNode)
        val matNodeMatArg = materialArg("material").executes {
            mat(
                it,
                MaterialArgumentType.getMaterial(it, "material"),
                BlockPosArgumentType.getBlockPos(it, "pos")
            )
        }.build()
        matNode.addChild(matNodeMatArg)
        val matNodeMatArgItemsArg = literal("items").executes {
            mat(
                it,
                MaterialArgumentType.getMaterial(it, "material"),
                BlockPosArgumentType.getBlockPos(it, "pos"),
                true
            )
        }.build()
        matNodeMatArg.addChild(matNodeMatArgItemsArg)


        val bothNode = literal("both").build()
        trimNodeBlockPodArg.addChild(bothNode)
        val bothNodeMatArg =
            materialArg("material").build()
        bothNode.addChild(bothNodeMatArg)
        val bothNodePatArg = patternArg("pattern")
            .executes {
                both(
                    it,
                    MaterialArgumentType.getMaterial(it, "material"),
                    PatterArgumentType.getPattern(it, "pattern"),
                    BlockPosArgumentType.getBlockPos(it, "pos")
                )
            }
            .build()
        bothNodeMatArg.addChild(bothNodePatArg)

        val bothNodePatArgItemsArg = literal("items")
            .executes {
                both(
                    it,
                    MaterialArgumentType.getMaterial(it, "material"),
                    PatterArgumentType.getPattern(it, "pattern"),
                    BlockPosArgumentType.getBlockPos(it, "pos"),
                    true
                )
            }
            .build()
        bothNodePatArg.addChild(bothNodePatArgItemsArg)
    }

    private fun all(c: CommandContext<ServerCommandSource>, pos: BlockPos, items: Boolean = false): Int {
        spawnArmorTrims(c.source, { true }, { true }, pos, false, items)
        return 1
    }

    private fun pat(
        c: CommandContext<ServerCommandSource>, pat: ArmorTrimPattern, pos: BlockPos, items: Boolean = false
    ): Int =
        spawnArmorTrims(c.source, { it == pat }, { true }, pos, false, items)


    private fun mat(
        c: CommandContext<ServerCommandSource>, mat: ArmorTrimMaterial, pos: BlockPos, items: Boolean = false
    ): Int =
        spawnArmorTrims(c.source, { true }, { it == mat }, pos, false, items)

    private fun both(
        c: CommandContext<ServerCommandSource>,
        mat: ArmorTrimMaterial,
        pat: ArmorTrimPattern,
        pos: BlockPos,
        items: Boolean = false
    ): Int =
        spawnArmorTrims(c.source, { it == pat }, { it == mat }, pos, true, items)


    private fun spawnArmorTrims(
        s: ServerCommandSource, patPred: (ArmorTrimPattern) -> Boolean, matPred: (ArmorTrimMaterial) -> Boolean,
        blockPos: BlockPos, row: Boolean, items: Boolean
    ): Int {
        val world = s.world
        val defaultedList = DefaultedList.of<ArmorTrimPermutation>()
        val patternsReg = world.registryManager.get(RegistryKeys.TRIM_PATTERN)
        val materialReg = world.registryManager.get(RegistryKeys.TRIM_MATERIAL)

        val pf = patternsReg.filter(patPred)
        val mf = materialReg.filter(matPred)

        pf.stream().forEachOrdered { pattern ->
            mf.stream().forEachOrdered { material ->
                defaultedList.add(
                    ArmorTrimPermutation(materialReg.wrapAsHolder(material), patternsReg.wrapAsHolder(pattern))
                )
            }
        }

        val i = ArmorMaterials.entries.size
        var j = 0
        var k = 0


        defaultedList.forEach { permutation ->
            ArmorMaterials.entries.toTypedArray().reversed().forEach { material ->
                    val x = blockPos.x + 0.5 - ((if (row) k else j) * 2.0)
                    val y = blockPos.y + (if (row) 0.0 else (k % i) * 3.0)
                    val z = blockPos.z + 0.5
                    val armorStandEntity = ArmorStandEntity(world, x, y, z)
                    armorStandEntity.yaw = 180.0f
                    armorStandEntity.setHideBasePlate(true)
                    armorStandEntity.setNoGravity(true)
                    armorStandEntity.addScoreboardTag("mod_placed")
                    if (items) {
                        armorStandEntity.equipStack(
                            EquipmentSlot.MAINHAND,
                            permutation.material.value().ingredient.value().defaultStack
                        )
                        armorStandEntity.equipStack(
                            EquipmentSlot.OFFHAND,
                            permutation.pattern.value().templateItem.value().defaultStack
                        )
                    }

                    EquipmentSlot.entries.toTypedArray().forEach { slot ->
                        val item = ARMOR_TYPES[Pair.of(material, slot)]
                        if (item != null) {
                            val itemStack = ItemStack(item)
                            ArmorTrimPermutation.tryAddPermutationToStack(world.registryManager, itemStack, permutation)
                            armorStandEntity.equipStack(slot, itemStack)
                        }
                    }
                    world.spawnEntity(armorStandEntity)
                    ++k
                }
            ++j
        }

        s.sendFeedback({ Text.literal("Armorstands with trimmed armor spawned around you") }, true)
        return 1
    }

    private val ARMOR_TYPES: Map<Pair<ArmorMaterial, EquipmentSlot>, Item> = Util.make(Maps.newHashMap()) { map ->
        map[Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.HEAD)] = Items.CHAINMAIL_HELMET
        map[Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.CHEST)] = Items.CHAINMAIL_CHESTPLATE
        map[Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.LEGS)] = Items.CHAINMAIL_LEGGINGS
        map[Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.FEET)] = Items.CHAINMAIL_BOOTS
        map[Pair.of(ArmorMaterials.IRON, EquipmentSlot.HEAD)] = Items.IRON_HELMET
        map[Pair.of(ArmorMaterials.IRON, EquipmentSlot.CHEST)] = Items.IRON_CHESTPLATE
        map[Pair.of(ArmorMaterials.IRON, EquipmentSlot.LEGS)] = Items.IRON_LEGGINGS
        map[Pair.of(ArmorMaterials.IRON, EquipmentSlot.FEET)] = Items.IRON_BOOTS
        map[Pair.of(ArmorMaterials.GOLD, EquipmentSlot.HEAD)] = Items.GOLDEN_HELMET
        map[Pair.of(ArmorMaterials.GOLD, EquipmentSlot.CHEST)] = Items.GOLDEN_CHESTPLATE
        map[Pair.of(ArmorMaterials.GOLD, EquipmentSlot.LEGS)] = Items.GOLDEN_LEGGINGS
        map[Pair.of(ArmorMaterials.GOLD, EquipmentSlot.FEET)] = Items.GOLDEN_BOOTS
        map[Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.HEAD)] = Items.NETHERITE_HELMET
        map[Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.CHEST)] = Items.NETHERITE_CHESTPLATE
        map[Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.LEGS)] = Items.NETHERITE_LEGGINGS
        map[Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.FEET)] = Items.NETHERITE_BOOTS
        map[Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.HEAD)] = Items.DIAMOND_HELMET
        map[Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.CHEST)] = Items.DIAMOND_CHESTPLATE
        map[Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.LEGS)] = Items.DIAMOND_LEGGINGS
        map[Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.FEET)] = Items.DIAMOND_BOOTS
        map[Pair.of(ArmorMaterials.LEATHER, EquipmentSlot.HEAD)] = Items.LEATHER_HELMET
        map[Pair.of(ArmorMaterials.LEATHER, EquipmentSlot.CHEST)] = Items.LEATHER_CHESTPLATE
        map[Pair.of(ArmorMaterials.LEATHER, EquipmentSlot.LEGS)] = Items.LEATHER_LEGGINGS
        map[Pair.of(ArmorMaterials.LEATHER, EquipmentSlot.FEET)] = Items.LEATHER_BOOTS
        map[Pair.of(ArmorMaterials.TURTLE, EquipmentSlot.HEAD)] = Items.TURTLE_HELMET
    }
}