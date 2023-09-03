package ram.talia.hexal.common.casting.actions.spells.motes

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.asActionResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import ram.talia.hexal.api.getMote
import ram.talia.hexal.api.getMoteOrItemEntityOrItemFrame

object OpMotesCombinable : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, ctx: CastingContext): List<Iota> {
        val typer = args.getMote(0, OpCombineMotes.argc) ?: return false.asActionResult
        val typee = args.getMoteOrItemEntityOrItemFrame(1, OpCombineMotes.argc) ?: return false.asActionResult

        return typee.flatMap({
            (typer.itemIndex != it.itemIndex && typer.typeMatches(it))
        }, {
            typer.typeMatches(it.item)
        }, {
            typer.typeMatches(it.item)
        }).asActionResult
    }
}