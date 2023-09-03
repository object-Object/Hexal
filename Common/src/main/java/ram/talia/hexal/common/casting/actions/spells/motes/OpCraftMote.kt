@file:Suppress("NAME_SHADOWING")

package ram.talia.hexal.common.casting.actions.spells.motes

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.SpellList
import at.petrak.hexcasting.api.spell.asActionResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.ListIota
import at.petrak.hexcasting.api.spell.iota.NullIota
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidIota
import com.mojang.datafixers.util.Either
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.getMoteOrList
import ram.talia.hexal.api.mediafieditems.ItemRecord
import ram.talia.hexal.api.mediafieditems.MediafiedItemManager
import ram.talia.hexal.api.mulBounded
import ram.talia.hexal.api.spell.casting.IMixinCastingContext
import ram.talia.hexal.api.spell.iota.MoteIota
import ram.talia.hexal.api.spell.mishaps.MishapNoBoundStorage

/**
 * Automate crafting with hex casting! Takes a list of item iotas that will be crafted with, returns a list containing the results of the craft.
 * The list should be one of the following:
 * \[item\], \[row of items\], \[\[row of items\], ...\], where the first list in the outermost list is the top row
 * e.g.
 * if you wanted to craft a diamond pickaxe, which has the following recipe
 * D D D
 *   S
 *   S
 * you'd pass the list \[\[D, D, D\], \[null, S\], \[null, S\]\].
 */
object OpCraftMote : ConstMediaAction {
    override val argc = 1
    override val mediaCost: Int
        get() = HexalConfig.server.craftItemCost

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun execute(args: List<Iota>, ctx: CastingContext): List<Iota> {
        val input = args.getMoteOrList(0, argc) ?: return listOf<Iota>().asActionResult
        val storage = (ctx as IMixinCastingContext).boundStorage ?: throw MishapNoBoundStorage(ctx.caster.position())
        if (!MediafiedItemManager.isStorageLoaded(storage))
            throw MishapNoBoundStorage(ctx.caster.position(), "storage_unloaded")

        val griddedIotas = makeItemIotaCraftingGrid(input)

        val container = CraftingContainer(AutoCraftingMenu(), 3, 3)

        for ((idx, iota) in griddedIotas.withIndex()) {
            if (iota != null)
                iota.record?.toStack()?.let { container.setItem(idx, it) }
        }

        val recman = ctx.world.recipeManager
        val recipes = recman.getAllRecipesFor(RecipeType.CRAFTING)
        val recipe = recipes.find { it.matches(container, ctx.world) } ?: return emptyList<Iota>().asActionResult

        val itemResult = recipe.assemble(container)
        val remainingItems = recipe.getRemainingItems(container).filter { item -> !item.isEmpty }

        val timesToCraft = getMinCount(griddedIotas)

        val moteIotaResult = MoteIota.makeIfStorageLoaded(ItemRecord(itemResult.item, itemResult.tag, itemResult.count.toLong().mulBounded(timesToCraft)), storage) ?: return emptyList<Iota>().asActionResult
        val remainingMoteIotas = remainingItems.map { MoteIota.makeIfStorageLoaded(ItemRecord(it.item, it.tag, it.count.toLong().mulBounded(timesToCraft)), storage)!! }.toMutableList()

        for (item in griddedIotas) item?.removeItems(timesToCraft)

        remainingMoteIotas.add(0, moteIotaResult)
        return remainingMoteIotas.asActionResult
    }

    private fun makeItemIotaCraftingGrid(input: Either<MoteIota, SpellList>): Array<MoteIota?> {
        val out = Array<MoteIota?>(9) { _ -> null }

        for ((idy, iota) in input.map({ listOf(IndexedValue(0, it)) }, { it.withIndex() })) {
            when (iota) {
                is MoteIota -> out[idy * 3] = iota.selfOrNull()
                is ListIota -> {
                    for ((idx, iota) in iota.list.withIndex()) {
                        when (iota) {
                            is MoteIota -> out[idy * 3 + idx] = iota.selfOrNull()
                            is NullIota -> out[idy * 3 + idx] = null
                            else -> throw MishapInvalidIota.of(input.map({ it }, { ListIota(it) }), 0, "crafting_recipe")
                        }

                    }
                }
                is NullIota -> out[idy * 3] = null
                else -> throw MishapInvalidIota.of(input.map({ it }, { ListIota(it) }), 0, "crafting_recipe")
            }
        }

        if (out.all { it == null })
            throw MishapInvalidIota.of(input.map({ it }, { ListIota(it) }), 0, "crafting_recipe")

        for (a in out.indices) {
            for (b in out.indices) {
                if (a != b && out[a] != null && out[a]?.itemIndex == out[b]?.itemIndex)
                    throw MishapInvalidIota.of(input.map({ it }, { ListIota(it) }), 0, "mote_duplicated")
            }
        }

        return out
    }

    private fun getMinCount(griddedIotas: Array<MoteIota?>): Long = griddedIotas.minOf { iota -> iota?.count ?: Long.MAX_VALUE }

    // from AE2 https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/9965a2fd4d3fbf9eadd0a0e7190e0812537f836e/src/main/java/appeng/menu/AutoCraftingMenu.java#L30
    private class AutoCraftingMenu : AbstractContainerMenu(null, 0) {
        override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

        override fun stillValid(player: Player) = false
    }
}