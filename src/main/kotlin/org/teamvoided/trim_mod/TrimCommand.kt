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
import java.awt.Color
import kotlin.math.abs

object TrimCommand {
    private var items = false;
    private var grid = false;

    fun reg(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val trimNode = literal("trim").build()
        dispatcher.root.addChild(trimNode)

        val itemNode = literal("items").executes(::toggleItems).build()
        trimNode.addChild(itemNode)
        val gridNode = literal("grid").executes(::toggleGrid).build()
        trimNode.addChild(gridNode)

        val trimNodeBlockPodArg = argument("pos", BlockPosArgumentType.blockPos()).build()
        trimNode.addChild(trimNodeBlockPodArg)

        val allNode = literal("all").executes { all(it, BlockPosArgumentType.getBlockPos(it, "pos")) }.build()
        trimNodeBlockPodArg.addChild(allNode)


        val patNode = literal("pattern").build()
        trimNodeBlockPodArg.addChild(patNode)
        val patNodePatArg = patternArg("pattern").executes {
            pat(
                it, PatterArgumentType.getPattern(it, "pattern"),
                BlockPosArgumentType.getBlockPos(it, "pos")
            )
        }.build()
        patNode.addChild(patNodePatArg)

        val matNode = literal("material").build()
        trimNodeBlockPodArg.addChild(matNode)
        val matNodeMatArg = materialArg("material").executes {
            mat(
                it, MaterialArgumentType.getMaterial(it, "material"),
                BlockPosArgumentType.getBlockPos(it, "pos")
            )
        }.build()
        matNode.addChild(matNodeMatArg)


        val bothNode = literal("both").build()
        trimNodeBlockPodArg.addChild(bothNode)
        val bothNodeMatArg =
            materialArg("material").build()
        bothNode.addChild(bothNodeMatArg)
        val bothNodePatArg = patternArg("pattern")
            .executes {
                both(
                    it, MaterialArgumentType.getMaterial(it, "material"),
                    PatterArgumentType.getPattern(it, "pattern"),
                    BlockPosArgumentType.getBlockPos(it, "pos")
                )
            }
            .build()
        bothNodeMatArg.addChild(bothNodePatArg)
    }

    private fun toggleItems(c: CommandContext<ServerCommandSource>): Int {
        val src = c.source
        items = !items
        src.sendSystemMessage(Text.translatable("Items toggled! [%s]", items))
        return 1
    }

    private fun toggleGrid(c: CommandContext<ServerCommandSource>): Int {
        val src = c.source
        grid = !grid
        src.sendSystemMessage(Text.translatable("Grid toggled! [%s]", grid))
        return 1
    }

    private fun all(c: CommandContext<ServerCommandSource>, pos: BlockPos): Int =
        spawnArmorTrims(c.source, { true }, { true }, pos, false)

    private fun pat(c: CommandContext<ServerCommandSource>, pat: ArmorTrimPattern, pos: BlockPos): Int =
        spawnArmorTrims(c.source, { it == pat }, { true }, pos, false)


    private fun mat(c: CommandContext<ServerCommandSource>, mat: ArmorTrimMaterial, pos: BlockPos): Int =
        spawnArmorTrims(c.source, { true }, { it == mat }, pos, false)

    private fun both(
        c: CommandContext<ServerCommandSource>, mat: ArmorTrimMaterial, pat: ArmorTrimPattern, pos: BlockPos
    ): Int =
        spawnArmorTrims(c.source, { it == pat }, { it == mat }, pos, true)


    private fun spawnArmorTrims(
        s: ServerCommandSource, patPred: (ArmorTrimPattern) -> Boolean, matPred: (ArmorTrimMaterial) -> Boolean,
        blockPos: BlockPos, row: Boolean
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

        defaultedList.sortedWith(compareBy { it.getColor()?.first })
        defaultedList.forEach { permutation ->
            ArmorMaterials.entries.toTypedArray().reversed().forEach { material ->
                val x = blockPos.x + 0.5 - (if (row) k else (if (grid) j % pf.size else j)) * 2.0
                val y = blockPos.y + (if (row) 0.0 else (k % i) * 3.0)
                val z = blockPos.z + 0.5 + (if (grid) (j / pf.size) * 5 else 0)
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

    private fun ArmorTrimPermutation.getColor(): Triple<Int, Int, Int>? {
        val textColor = this.material.value().description.style.color ?: return null
        return Color(textColor.rgb).toHSL()
    }
    private fun Color.toHSL(): Triple<Int, Int, Int> {
        // Convert RGB [0, 255] range to [0, 1]
        val rf = this.red / 255.0
        val gf = this.green / 255.0
        val bf = this.blue / 255.0

        // Get the min and max of r,g,b
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)

        // Lightness is the average of the largest and smallest color components
        val lum = (max + min) / 2

        val hue: Double
        val sat: Double

        if (max == min) { // No saturation
            hue = 0.0
            sat = 0.0
        } else {
            val c = max - min // Chroma
            // Saturation is simply the chroma scaled to fill the interval [0, 1]
            sat = c / (1 - abs(2 * lum - 1))
            hue = when (max) {
                rf -> 60 * ((gf - bf) / c + (if (gf < bf) 6 else 0))
                gf -> 60 * ((bf - rf) / c + 2)
                else -> 60 * ((rf - gf) / c + 4)
            }
        }
        // Convert hue to degrees, sat and lum to percentage
        val h = ((hue + 360) % 360).toInt() // Ensure hue is within [0, 360)
        val s = (sat * 100).toInt()
        val l = (lum * 100).toInt()

        return Triple(h, s, l)
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