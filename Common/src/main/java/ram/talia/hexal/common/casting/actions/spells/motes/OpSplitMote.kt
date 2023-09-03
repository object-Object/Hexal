@file:Suppress("CAST_NEVER_SUCCEEDS")

package ram.talia.hexal.common.casting.actions.spells.motes

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.NullIota
import ram.talia.hexal.api.getMote
import ram.talia.hexal.api.getStrictlyPositiveLong
import ram.talia.hexal.api.mediafieditems.MediafiedItemManager
import ram.talia.hexal.api.spell.casting.IMixinCastingContext
import ram.talia.hexal.api.spell.mishaps.MishapNoBoundStorage
import ram.talia.hexal.api.spell.mishaps.MishapStorageFull

object OpSplitMote : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, ctx: CastingContext): List<Iota> {
        val item = args.getMote(0, argc) ?: return listOf(NullIota())
        val toSplitOff = args.getStrictlyPositiveLong(1, argc)

        val storage = (ctx as IMixinCastingContext).boundStorage ?: item.itemIndex.storage
        if (!MediafiedItemManager.isStorageLoaded(storage))
            throw MishapNoBoundStorage(ctx.caster.position(), "storage_unloaded")
        if (MediafiedItemManager.isStorageFull(storage) != false) // if this is somehow null we should still throw an error here, things have gone pretty wrong
            throw MishapStorageFull(ctx.caster.position())

        val split = item.splitOff(toSplitOff, storage) ?: return listOf(item.copy(), NullIota())
        return listOf(item.copy(), split.copy())
    }
}