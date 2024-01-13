package org.teamvoided.trim_mod

import com.google.common.collect.Maps
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.datafixers.util.Pair
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.*
import net.minecraft.item.trim.ArmorTrimMaterial
import net.minecraft.item.trim.ArmorTrimPattern
import net.minecraft.item.trim.ArmorTrimPermutation
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.collection.DefaultedList
import org.teamvoided.trim_mod.MaterialArgumentType.materialArg
import org.teamvoided.trim_mod.PatterArgumentType.patternArg

object TrimCommand {

    fun reg(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val trimNode = literal("trim").build()
        dispatcher.root.addChild(trimNode)

        val allNode = literal("all").executes(this::all).build()
        trimNode.addChild(allNode)


        val patNode = literal("pattern").build()
        trimNode.addChild(patNode)
        val patNodePatArg =
            patternArg("pattern").executes { pat(it, PatterArgumentType.getPattern(it, "pattern")) }.build()
        patNode.addChild(patNodePatArg)


        val matNode = literal("material").build()
        trimNode.addChild(matNode)
        val matNodeMatArg =
            materialArg("material").executes { mat(it, MaterialArgumentType.getMaterial(it, "material")) }.build()
        matNode.addChild(matNodeMatArg)


        val bothNode = literal("both").build()
        trimNode.addChild(bothNode)
        val bothNodeMatArg =
            materialArg("material").build()
        bothNode.addChild(bothNodeMatArg)
        val bothNodePatArg =
            patternArg("pattern")
                .executes {
                    both(
                        it,
                        MaterialArgumentType.getMaterial(it, "material"),
                        PatterArgumentType.getPattern(it, "pattern")
                    )
                }
                .build()
        bothNodeMatArg.addChild(bothNodePatArg)
    }

    private fun all(c: CommandContext<ServerCommandSource>): Int {
        spawnArmorTrims(c.source, { true }, { true })
        return 1
    }

    private fun pat(c: CommandContext<ServerCommandSource>, pat: ArmorTrimPattern): Int {
        spawnArmorTrims(c.source, { it == pat }, { true })
        return 1
    }

    private fun mat(c: CommandContext<ServerCommandSource>, mat: ArmorTrimMaterial): Int {
        spawnArmorTrims(c.source, { true }, { it == mat })
        return 1
    }

    private fun both(c: CommandContext<ServerCommandSource>, mat: ArmorTrimMaterial, pat: ArmorTrimPattern): Int {
        spawnArmorTrims(c.source, { it == pat }, { it == mat })
        return 1
    }


    private fun spawnArmorTrims(
        s: ServerCommandSource, patPred: (ArmorTrimPattern) -> Boolean, matPred: (ArmorTrimMaterial) -> Boolean
    ): Int {
        val world = s.world
        val player = s.player!!
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

        val blockPos = player.blockPos
        val i = ArmorMaterials.entries.size - 1
        var j = 0
        var k = 0


        defaultedList.forEach { trim ->
            ArmorMaterials.entries.toTypedArray().reversed().forEach { material ->
                if (material !== ArmorMaterials.LEATHER) {
                    val x = blockPos.x + 0.5 - (j * 2.0)
                    val y = blockPos.y + (k % i) * 3.0
                    val z = blockPos.z + 0.5
                    val armorStandEntity = ArmorStandEntity(world, x, y, z)
                    armorStandEntity.yaw = 180.0f
                    armorStandEntity.setHideBasePlate(true)
                    armorStandEntity.setNoGravity(true)

                    EquipmentSlot.entries.toTypedArray().forEach { slot ->
                        val item = ARMOR_TYPES[Pair.of(material, slot)]
                        if (item != null) {
                            val itemStack = ItemStack(item)
                            ArmorTrimPermutation.tryAddPermutationToStack(world.registryManager, itemStack, trim)
                            armorStandEntity.equipStack(slot, itemStack)
                        }
                    }
                    world.spawnEntity(armorStandEntity)
                    ++k
                }
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
        map[Pair.of(ArmorMaterials.TURTLE, EquipmentSlot.HEAD)] = Items.TURTLE_HELMET
    }
}